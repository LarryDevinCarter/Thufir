package com.larrydevincarter.thufir.tools;

import com.larrydevincarter.thufir.models.OrderLeg;
import com.larrydevincarter.thufir.models.dtos.OrderRequestDto;
import com.larrydevincarter.thufir.models.entities.Position;
import com.larrydevincarter.thufir.services.TastytradeService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class TastytradeTools {

    private final TastytradeService accountService;
    private final ProfitGoalTools profitGoalTools;

    @Tool("Get the current available cash / liquid buying power from tastytrade account in USD")
    public String getAvailableCash() {
        try {
            BigDecimal cash = accountService.getAvailableCash();
            return cash.toPlainString();
        } catch (Exception e) {
            return "Error fetching cash balance: " + e.getMessage();
        }
    }

    @Tool("Get full current account balances snapshot as JSON-like string")
    public String getFullBalances() {
        try {
            Map<String, Object> balances = accountService.getCurrentBalances();
            return balances.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("Get current positions as JSON-like string")
    public String getCurrentPositions() {
        try {
            List<Position> positions = accountService.fetchAndSavePositions();
            return positions.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    @Tool("""
            Preview a buy order (dry-run) without executing it.
            Use this FIRST to check buying power impact and estimated fill.
            Params:
            • symbol: e.g. NVDA, BTC/USD, TSLA
            • quantity: number of shares (whole for equity) or amount (crypto)
            • isCrypto: true for BTC/USD, ETH/USD, DOGE/USD
            Returns: JSON-like string with preview result or error
            """)
    public String previewBuyOrder(String symbol, String quantityStr, boolean isCrypto) {
        try {
            BigDecimal qty = new BigDecimal(quantityStr);
            OrderLeg leg = OrderLeg.builder()
                    .instrumentType(isCrypto ? "Cryptocurrency" : "Equity")
                    .symbol(symbol.toUpperCase())
                    .quantity(qty)
                    .action("Buy to Open")
                    .build();

            OrderRequestDto req = OrderRequestDto.builder()
                    .timeInForce("Day")
                    .orderType("Market")
                    .legs(List.of(leg))
                    .build();

            Map<String, Object> result = accountService.placeOrderDryRun(req);
            return result.toString();
        } catch (Exception e) {
            return "Error previewing buy order: " + e.getMessage();
        }
    }

    @Tool("""
            INTERNAL - Execute a confirmed buy order.
            ONLY call AFTER Larry explicitly confirms (e.g. "yes execute", "place the order").
            Use the exact same params as previewBuyOrder.
            Returns: order ID or confirmation message
            """)
    public String executeBuyOrder(String symbol, String quantityStr, boolean isCrypto) {
        try {
            BigDecimal qty = new BigDecimal(quantityStr);
            OrderLeg leg = OrderLeg.builder()
                    .instrumentType(isCrypto ? "Cryptocurrency" : "Equity")
                    .symbol(symbol.toUpperCase())
                    .quantity(qty)
                    .action("Buy to Open")
                    .build();

            OrderRequestDto req = OrderRequestDto.builder()
                    .timeInForce("Day")
                    .orderType("Market")
                    .legs(List.of(leg))
                    .build();

            Map<String, Object> result = accountService.placeOrder(req);
            return result.toString();
        } catch (Exception e) {
            return "Error placing buy order: " + e.getMessage();
        }
    }

    @Tool("""
            Get current mark prices (mid/last trade) for stocks and/or crypto in one call.

            Params:
            • symbols: comma-separated list of tickers, e.g. "NVDA,TSLA,BTC,ETH,DOGE,RKLB"
              - Stocks: use plain ticker (NVDA, TSLA, GOOGL, etc.)
              - Crypto: use BTC, ETH, DOGE (tool adds /USD internally)

            Returns: JSON-like string e.g. {NVDA=1365.42, BTC=92840.50, ...} or error message
            Use this before calculating remaining-to-target, suggested quantities, profit triggers, or any value-based recommendation.
            """)
    public String getCurrentMarkPrices(String symbols) {
        try {
            if (symbols == null || symbols.trim().isEmpty()) {
                return "{}";
            }

            Set<String> all = Arrays.stream(symbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            Set<String> equities = new HashSet<>();
            Set<String> cryptos = new HashSet<>();

            Set<String> knownCryptos = Set.of("BTC", "ETH", "DOGE");

            for (String s : all) {
                if (knownCryptos.contains(s)) {
                    cryptos.add(s);
                } else {
                    equities.add(s);
                }
            }

            Map<String, BigDecimal> prices = accountService.getCurrentMarkPrices(equities, cryptos);
            return prices.toString();  // clean: {NVDA=1365.42, BTC=92840.50, ...}
        } catch (Exception e) {
            log.warn("Price fetch failed for symbols: {}", symbols, e);
            return "Error fetching prices: " + e.getMessage();
        }
    }

    @Tool("""
    Get the full sequence of recommended buys to deploy available cash following strict sequential priority rules.
    Funds ONE category at a time until it reaches or exceeds target before moving to the next.
    Accepts small overage to hit target with whole shares/crypto blocks.
    Stops sequence if a category cannot be fully funded with remaining cash.
    Returns JSON-like string with array of recommendations + summary.
""")
    public String getNextBuySequenceRecommendations() {
        try {
            String pStr = profitGoalTools.getCurrentProfitGoal();
            if ("NOT_SET".equals(pStr)) {
                return "{\"recommendations\":[], \"message\":\"Profit goal (P) not set yet. Cannot recommend buys.\"}";
            }
            BigDecimal P = new BigDecimal(pStr);

            List<Position> positions = accountService.fetchAndSavePositions();
            String symbols = "NVDA,BTC,ETH,DOGE,RKLB,TSLA,GOOGL,GOOG,NIKL,ABEV,AIT,ALKS,ASR,BKR,CHRD,CRUS,CTRA,DECK,EOG,GIB,HLI,HMY,IDCC,INTU,LULU,MATX,MNSO,NTES,OVV,RDY,RMD,TSM,TW,UTHR";
            String pricesJson = getCurrentMarkPrices(symbols);
            Map<String, BigDecimal> prices = parsePriceMap(pricesJson);

            BigDecimal cashRemaining = new BigDecimal(getAvailableCash());
            if (cashRemaining.compareTo(BigDecimal.valueOf(10)) < 0) {
                return "{\"recommendations\":[], \"message\":\"Available cash too low (" + cashRemaining + ").\"}";
            }

            Map<String, BigDecimal> targets = createCategoryTargets(P);
            List<String> priority = List.of("NVDA", "BTC", "RKLB", "TSLA", "ETH", "DOGE", "GOOGL/GOOG", "NIKL", "OTHER_BASKET");
            Map<String, BigDecimal> currentMVs = computeCategoryMarketValues(positions);

            List<Map<String, Object>> recommendations = new ArrayList<>();

            for (String category : priority) {
                BigDecimal currentMV = currentMVs.getOrDefault(category, BigDecimal.ZERO);
                BigDecimal target = targets.get(category);

                if (currentMV.compareTo(target) >= 0) continue;

                Map<String, Object> buyRec = buildBuyToReachTarget(category, cashRemaining, prices, positions, currentMV, target);

                if (buyRec == null || buyRec.isEmpty()) {
                    break;
                }

                BigDecimal estCost = new BigDecimal((String) buyRec.get("estCost"));
                BigDecimal projectedMV = currentMV.add(estCost);

                recommendations.add(buyRec);

                cashRemaining = cashRemaining.subtract(estCost);
                currentMVs.put(category, projectedMV);

                if (projectedMV.compareTo(target) < 0) {
                    break;
                }

                if (cashRemaining.compareTo(BigDecimal.valueOf(10)) < 0) break;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("recommendations", recommendations);
            result.put("totalToDeploy", new BigDecimal(getAvailableCash()).subtract(cashRemaining).toPlainString());
            result.put("cashRemainingAfter", cashRemaining.toPlainString());

            String msg = recommendations.isEmpty()
                    ? "No buys recommended (all categories funded or insufficient cash)."
                    : recommendations.size() + " sequential buy" + (recommendations.size() > 1 ? "s" : "") + " recommended to reach targets.";

            result.put("message", msg);

            return result.toString();

        } catch (Exception e) {
            log.error("Error generating buy sequence", e);
            return "{\"recommendations\":[], \"message\":\"Error: " + e.getMessage() + "\"}";
        }
    }

    private Map<String, Object> buildBuyToReachTarget(String category, BigDecimal maxCash, Map<String, BigDecimal> prices,
                                                      List<Position> positions, BigDecimal currentMV, BigDecimal target) {
        Map<String, Object> rec = new HashMap<>();
        rec.put("category", category);

        BigDecimal gap = target.subtract(currentMV).max(BigDecimal.ZERO);
        if (gap.compareTo(BigDecimal.ZERO) <= 0) return null;

        boolean isCrypto = Set.of("BTC", "ETH", "DOGE").contains(category);

        if (isCrypto) {
            BigDecimal price = prices.get(category);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) return null;

            BigDecimal blockSize = getCryptoBlockSize(price);
            BigDecimal blocksNeeded = gap.add(price.multiply(blockSize).subtract(BigDecimal.ONE))
                    .divide(price.multiply(blockSize), 0, RoundingMode.CEILING);

            BigDecimal qty = blocksNeeded.multiply(blockSize);
            BigDecimal cost = qty.multiply(price);

            if (cost.compareTo(maxCash) > 0) {
                BigDecimal maxBlocks = maxCash.divide(price.multiply(blockSize), 0, RoundingMode.FLOOR);
                if (maxBlocks.compareTo(BigDecimal.ZERO) <= 0) return null;
                qty = maxBlocks.multiply(blockSize);
                cost = qty.multiply(price);
            }

            rec.put("symbol", category + "/USD");
            rec.put("quantity", qty.toPlainString());
            rec.put("priceUsed", price.toPlainString());
            rec.put("estCost", cost.toPlainString());
            rec.put("isCrypto", true);
            return rec;
        }

        BigDecimal price;
        String symbolToBuy;

        if ("GOOGL/GOOG".equals(category)) {
            BigDecimal googlP = prices.get("GOOGL");
            BigDecimal googP  = prices.get("GOOG");
            if (googlP == null || googP == null) return null;

            String cheaperSym = googlP.compareTo(googP) <= 0 ? "GOOGL" : "GOOG";
            BigDecimal cheaperPrice = googlP.min(googP);
            BigDecimal expensivePrice = googlP.max(googP);

            BigDecimal cheaperMV = getMVForSingleSymbol(positions, cheaperSym);
            BigDecimal expensiveMV = getMVForSingleSymbol(positions, cheaperSym.equals("GOOGL") ? "GOOG" : "GOOGL");

            if (cheaperMV.compareTo(expensiveMV.add(expensivePrice)) < 0) {
                symbolToBuy = cheaperSym;
                price = cheaperPrice;
            } else {
                symbolToBuy = cheaperSym.equals("GOOGL") ? "GOOG" : "GOOGL";
                price = expensivePrice;
            }
        } else if ("OTHER_BASKET".equals(category)) {
            Set<String> basket = Set.of("ABEV","AIT","ALKS","ASR","BKR","CHRD","CRUS","CTRA","DECK","EOG","GIB","HLI","HMY","IDCC","INTU","LULU","MATX","MNSO","NTES","OVV","RDY","RMD","TSM","TW","UTHR");
            List<Map.Entry<String, BigDecimal>> sorted = basket.stream()
                    .filter(prices::containsKey)
                    .map(s -> Map.entry(s, prices.get(s)))
                    .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                    .sorted(Map.Entry.comparingByValue())
                    .toList();

            if (sorted.isEmpty()) return null;
            symbolToBuy = sorted.get(0).getKey();
            price = sorted.get(0).getValue();
        } else {
            symbolToBuy = category;
            price = prices.get(category);
            if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) return null;
        }

        BigDecimal sharesNeeded = gap.add(price.subtract(BigDecimal.ONE))
                .divide(price, 0, RoundingMode.CEILING);

        BigDecimal cost = sharesNeeded.multiply(price);

        if (cost.compareTo(maxCash) > 0) {
            BigDecimal maxShares = maxCash.divide(price, 0, RoundingMode.FLOOR);
            if (maxShares.compareTo(BigDecimal.ZERO) <= 0) return null;
            sharesNeeded = maxShares;
            cost = sharesNeeded.multiply(price);
        }

        rec.put("symbol", symbolToBuy);
        rec.put("quantity", sharesNeeded.toPlainString());
        rec.put("priceUsed", price.toPlainString());
        rec.put("estCost", cost.toPlainString());
        rec.put("isCrypto", false);

        return rec;
    }

    private BigDecimal getCryptoBlockSize(BigDecimal price) {
        if (price.compareTo(BigDecimal.valueOf(318)) <= 0) return BigDecimal.ONE;
        BigDecimal block = BigDecimal.ONE;
        BigDecimal threshold = BigDecimal.valueOf(318);
        while (price.compareTo(threshold) > 0) {
            block = block.divide(BigDecimal.TEN, 10, RoundingMode.HALF_DOWN);
            threshold = threshold.multiply(BigDecimal.TEN);
        }
        return block;
    }

    private Map<String, BigDecimal> parsePriceMap(String jsonLike) {
        Map<String, BigDecimal> map = new HashMap<>();
        String cleaned = jsonLike.replaceAll("[{}\"]", "").trim();
        if (cleaned.isEmpty()) return map;
        for (String pair : cleaned.split(",")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                String key = kv[0].trim().toUpperCase();
                try {
                    map.put(key, new BigDecimal(kv[1].trim()));
                } catch (Exception ignored) {
                }
            }
        }
        return map;
    }

    private Map<String, BigDecimal> createCategoryTargets(BigDecimal P) {
        Map<String, BigDecimal> t = new HashMap<>();
        t.put("NVDA", P);
        t.put("BTC", P);
        t.put("RKLB", P);
        t.put("TSLA", P.multiply(BigDecimal.TWO));
        t.put("ETH", P);
        t.put("DOGE", P);
        t.put("GOOGL/GOOG", P);
        t.put("NIKL", P);
        t.put("OTHER_BASKET", P);
        return t;
    }

    private Map<String, BigDecimal> computeCategoryMarketValues(List<Position> positions) {
        Map<String, BigDecimal> values = new HashMap<>();
        Set<String> basket = Set.of("ABEV", "AIT", "ALKS", "ASR", "BKR", "CHRD", "CRUS", "CTRA", "DECK", "EOG", "GIB", "HLI", "HMY", "IDCC", "INTU", "LULU", "MATX", "MNSO", "NTES", "OVV", "RDY", "RMD", "TSM", "TW", "UTHR");

        for (Position p : positions) {
            String sym = p.getSymbol().toUpperCase();
            BigDecimal mv = p.getMarketValue();

            if (basket.contains(sym)) {
                values.merge("OTHER_BASKET", mv, BigDecimal::add);
            } else if ("GOOGL".equals(sym) || "GOOG".equals(sym)) {
                values.merge("GOOGL/GOOG", mv, BigDecimal::add);
            } else {
                values.merge(sym, mv, BigDecimal::add);
            }
        }
        return values;
    }

    private BigDecimal getMVForSingleSymbol(List<Position> positions, String symbol) {
        return positions.stream()
                .filter(p -> p.getSymbol().equalsIgnoreCase(symbol))
                .map(Position::getMarketValue)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}