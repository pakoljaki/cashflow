package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
import com.akosgyongyosi.cashflow.service.fx.FxConversionContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class OneTimeTransactionStrategy implements ForecastStrategy {

    @Override
    public boolean supports(LineItemType type) {
        return type == LineItemType.ONE_TIME;
    }

    @Override
    public void applyForecast(CashflowPlan plan, PlanLineItem item) {
        LocalDate txDate = item.getTransactionDate();
        if (txDate == null) {
            txDate = plan.getStartDate();
        }

        if (!item.getIsApplied()) {
            item.setIsApplied(true);
            HistoricalTransaction newTx = new HistoricalTransaction();
            newTx.setCashflowPlan(plan);
            newTx.setTransactionDate(txDate);

            BigDecimal nativeAmt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            Currency fromCurrency = item.getCurrency() != null ? item.getCurrency() : FxConversionContext.base();
            BigDecimal amountBase = FxConversionContext.convert(txDate, fromCurrency, nativeAmt);

            newTx.setAmount(amountBase);
            newTx.setCategory(item.getCategory());

            plan.getBaselineTransactions().add(newTx);
        } else {
            return;
        }
    }
}
