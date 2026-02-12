# Investment Plan – Buy-and-Hold with Sequential Funding & Partial Profit Taking

## Overview
This is a disciplined, long-term buy-and-hold strategy with 10 prioritized asset categories.  
The goal is to fund each category until its cost basis reaches a target where **doubling in price** would allow you to sell a portion realizing your profit goal (P) while leaving the original investment value (cost basis) intact in that category.

New funds are always invested into the **earliest category in the priority list** that is still below its target cost basis.  
Once all 10 categories reach their targets, investing stops completely.

## Profit Goal
- Let **P** = your defined profit goal (the dollar amount you ultimately want to realize).  
- All targets and exit logic are expressed in terms of P.

## Asset Categories & Priority Order
1. NVDA  
2. BTC  
3. RKLB  
4. TSLA (double allocation – treated as two standard slots)  
5. ETH  
6. DOGE  
7. GOOGL/GOOG (treated as one combined category)  
8. NIKL  
9. Other stocks (one combined basket category)  
   - ABEV, AIT, ALKS, ASR, BKR, CHRD, CRUS, CTRA, DECK, EOG, GIB, HLI, HMY, IDCC, INTU, LULU, MATX, MNSO, NTES, OVV, RDY, RMD, TSM, TW, UTHR

## Target Cost Basis per Category
| Category       | Target Cost Basis | Notes                                      |
|----------------|-------------------|--------------------------------------------|
| 1–3, 5–8       | P                 | Standard target                            |
| TSLA           | 2P                | Double allocation                          |
| GOOGL/GOOG     | P                 | Combined across both tickers               |
| Other stocks   | P                 | Combined across all stocks in the basket   |

## Investment Sequence Rule
1. When new capital is available, start at the top of the list (NVDA).  
2. Invest into the **first category** whose current cost basis is < target.  
3. Add funds until that category reaches (or would exceed) its target — never buy more than needed to hit the target.  
4. Move to the next underfunded category in order.  
5. Repeat until **all 10 categories** are at or above target → stop investing.

## Buying Rules
### General
- Only buy into the current priority (earliest underfunded) category.  
- Never exceed the amount needed to reach the category target.

### Stocks (whole shares only)
- NVDA, RKLB, TSLA, GOOGL, GOOG, NIKL, and all "Other stocks" → **whole shares only** (no fractionals).

### Crypto (BTC, ETH, DOGE)
- Price ≤ $318 → buy whole units (implied).  
- Price > $318 → buy fractionals in powers-of-10 blocks:  
  - $318 < price < $3,180   → blocks of **0.1**  
  - $3,180 < price < $31,800 → blocks of **0.01**  
  - $31,800 < price < $318,000 → blocks of **0.001**  
  - and so on  
- Example: $700 available, asset = $2,900 → buy **0.2** ($580), remainder unspent if that's all that's needed.

### TSLA (Double Allocation)
- Target cost basis = **2P**  
- Buy whole shares until total invested in TSLA reaches 2P.  
- Treated as one position for buying/selling decisions.

### GOOGL / GOOG (Combined Category)
- Target = **P** (combined cost basis of GOOGL + GOOG)  
- **Buying preference**: Always favor the cheaper ticker to keep values somewhat balanced.  
  - Let A = cheaper ticker price, B = more expensive ticker price  
  - If value owned in cheaper < (value owned in more expensive + 1 share of more expensive), buy 1 share of cheaper.  
  - Otherwise buy 1 share of more expensive.  
- Example: GOOGL $200, GOOG $220 → buy GOOGL until its value > (GOOG value + $220), then buy GOOG, repeat.

### Other Stocks Basket
- Target = **P** (combined cost basis across all listed stocks)  
- Rank stocks by current share price (ascending).  
- Apply similar balancing logic as GOOGL/GOOG: prefer adding to lower-priced / under-weighted positions in the basket.  
- Buy whole shares only.

## Taking Profit Rules
**Trigger condition** (per category):  
Current market value (V) ≥ cost basis (I) + P  
→ You can sell a portion to realize **exactly P** in profit while leaving ≥ I still invested in that category.

**Execution**:
- Sell only enough to take **P** in realized gains (not principal).  
- Stocks → sell whole shares only.  
- Crypto → sell in the same fractional blocks allowed for buying (if >$318).  
- After sale, remaining position should be valued at ≥ original cost basis I.

**Special Cases**:
- **TSLA**: Trigger requires gains ≥ **2P** (due to double allocation). Sell only enough to realize **P** (not the full 2P).  
- **GOOGL/GOOG**: Evaluate combined value. Sell from one or both tickers to realize P.  
- **Other stocks basket**: Evaluate combined value. Sell shares from appropriate stocks (following price/balance logic) to realize P.

## Summary – Core Philosophy
- Sequential, disciplined accumulation  
- No over-allocation beyond targets  
- Profits taken category-by-category only when doubling would safely deliver P  
- Whole-share discipline on equities; controlled fractionals on high-priced crypto  
- Double weighting for TSLA, balanced sub-allocation for multi-ticker categories