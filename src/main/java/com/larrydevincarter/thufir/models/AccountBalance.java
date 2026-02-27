package com.larrydevincarter.thufir.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Builder
@Jacksonized
public record AccountBalance(

        @JsonProperty("account-number")
        String accountNumber,

        @JsonProperty("available-trading-funds")
        BigDecimal availableTradingFunds,

        @JsonProperty("bond-margin-requirement")
        BigDecimal bondMarginRequirement,

        @JsonProperty("cash-available-to-withdraw")
        BigDecimal cashAvailableToWithdraw,

        @JsonProperty("cash-balance")
        BigDecimal cashBalance,

        @JsonProperty("cash-settle-balance")
        BigDecimal cashSettleBalance,

        @JsonProperty("closed-loop-available-balance")
        BigDecimal closedLoopAvailableBalance,

        @JsonProperty("cryptocurrency-margin-requirement")
        BigDecimal cryptocurrencyMarginRequirement,

        @JsonProperty("currency")
        String currency,

        @JsonProperty("day-equity-call-value")
        BigDecimal dayEquityCallValue,

        @JsonProperty("day-trade-excess")
        BigDecimal dayTradeExcess,

        @JsonProperty("day-trading-buying-power")
        BigDecimal dayTradingBuyingPower,

        @JsonProperty("day-trading-call-value")
        BigDecimal dayTradingCallValue,

        @JsonProperty("derivative-buying-power")
        BigDecimal derivativeBuyingPower,

        @JsonProperty("equity-buying-power")
        BigDecimal equityBuyingPower,

        @JsonProperty("equity-offering-margin-requirement")
        BigDecimal equityOfferingMarginRequirement,

        @JsonProperty("fixed-income-security-margin-requirement")
        BigDecimal fixedIncomeSecurityMarginRequirement,

        @JsonProperty("futures-margin-requirement")
        BigDecimal futuresMarginRequirement,

        @JsonProperty("intraday-equities-cash-amount")
        BigDecimal intradayEquitiesCashAmount,

        @JsonProperty("intraday-equities-cash-effect")
        String intradayEquitiesCashEffect,

        @JsonProperty("intraday-equities-cash-effective-date")
        LocalDate intradayEquitiesCashEffectiveDate,

        @JsonProperty("intraday-futures-cash-amount")
        BigDecimal intradayFuturesCashAmount,

        @JsonProperty("intraday-futures-cash-effect")
        String intradayFuturesCashEffect,

        @JsonProperty("intraday-futures-cash-effective-date")
        LocalDate intradayFuturesCashEffectiveDate,

        @JsonProperty("long-bond-value")
        BigDecimal longBondValue,

        @JsonProperty("long-cryptocurrency-value")
        BigDecimal longCryptocurrencyValue,

        @JsonProperty("long-derivative-value")
        BigDecimal longDerivativeValue,

        @JsonProperty("long-equity-value")
        BigDecimal longEquityValue,

        @JsonProperty("long-fixed-income-security-value")
        BigDecimal longFixedIncomeSecurityValue,

        @JsonProperty("long-futures-derivative-value")
        BigDecimal longFuturesDerivativeValue,

        @JsonProperty("long-futures-value")
        BigDecimal longFuturesValue,

        @JsonProperty("long-margineable-value")
        BigDecimal longMargineableValue,

        @JsonProperty("maintenance-call-value")
        BigDecimal maintenanceCallValue,

        @JsonProperty("maintenance-requirement")
        BigDecimal maintenanceRequirement,

        @JsonProperty("margin-equity")
        BigDecimal marginEquity,

        @JsonProperty("margin-settle-balance")
        BigDecimal marginSettleBalance,

        @JsonProperty("net-liquidating-value")
        BigDecimal netLiquidatingValue,

        @JsonProperty("pending-cash")
        BigDecimal pendingCash,

        @JsonProperty("pending-cash-effect")
        String pendingCashEffect,

        @JsonProperty("previous-date-cryptocurrency-fiat-effective-date")
        LocalDate previousDateCryptocurrencyFiatEffectiveDate,

        @JsonProperty("previous-day-cryptocurrency-fiat-amount")
        BigDecimal previousDayCryptocurrencyFiatAmount,

        @JsonProperty("previous-day-cryptocurrency-fiat-effect")
        String previousDayCryptocurrencyFiatEffect,

        @JsonProperty("reg-t-call-value")
        BigDecimal regTCallValue,

        @JsonProperty("short-cryptocurrency-value")
        BigDecimal shortCryptocurrencyValue,

        @JsonProperty("short-derivative-value")
        BigDecimal shortDerivativeValue,

        @JsonProperty("short-equity-value")
        BigDecimal shortEquityValue,

        @JsonProperty("short-futures-derivative-value")
        BigDecimal shortFuturesDerivativeValue,

        @JsonProperty("short-futures-value")
        BigDecimal shortFuturesValue,

        @JsonProperty("short-margineable-value")
        BigDecimal shortMargineableValue,

        @JsonProperty("sma-equity-option-buying-power")
        BigDecimal smaEquityOptionBuyingPower,

        @JsonProperty("special-memorandum-account-apex-adjustment")
        BigDecimal specialMemorandumAccountApexAdjustment,

        @JsonProperty("special-memorandum-account-value")
        BigDecimal specialMemorandumAccountValue,

        @JsonProperty("total-settle-balance")
        BigDecimal totalSettleBalance,

        @JsonProperty("unsettled-cryptocurrency-fiat-amount")
        BigDecimal unsettledCryptocurrencyFiatAmount,

        @JsonProperty("unsettled-cryptocurrency-fiat-effect")
        String unsettledCryptocurrencyFiatEffect,

        @JsonProperty("used-derivative-buying-power")
        BigDecimal usedDerivativeBuyingPower,

        @JsonProperty("snapshot-date")
        LocalDate snapshotDate,

        @JsonProperty("time-of-day")
        String timeOfDay,

        @JsonProperty("reg-t-margin-requirement")
        BigDecimal regTMarginRequirement,

        @JsonProperty("futures-overnight-margin-requirement")
        BigDecimal futuresOvernightMarginRequirement,

        @JsonProperty("futures-intraday-margin-requirement")
        BigDecimal futuresIntradayMarginRequirement,

        @JsonProperty("maintenance-excess")
        BigDecimal maintenanceExcess,

        @JsonProperty("pending-margin-interest")
        BigDecimal pendingMarginInterest,

        @JsonProperty("apex-starting-day-margin-equity")
        BigDecimal apexStartingDayMarginEquity,

        @JsonProperty("buying-power-adjustment")
        BigDecimal buyingPowerAdjustment,

        @JsonProperty("buying-power-adjustment-effect")
        String buyingPowerAdjustmentEffect,

        @JsonProperty("effective-cryptocurrency-buying-power")
        BigDecimal effectiveCryptocurrencyBuyingPower,

        @JsonProperty("total-pending-liquidity-pool-rebate")
        BigDecimal totalPendingLiquidityPoolRebate,

        @JsonProperty("long-index-derivative-value")
        BigDecimal longIndexDerivativeValue,

        @JsonProperty("short-index-derivative-value")
        BigDecimal shortIndexDerivativeValue,

        @JsonProperty("updated-at")
        OffsetDateTime updatedAt

) {}