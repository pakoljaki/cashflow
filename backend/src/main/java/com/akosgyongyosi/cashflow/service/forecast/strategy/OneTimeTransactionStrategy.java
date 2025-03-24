package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
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

        HistoricalTransaction newTx = new HistoricalTransaction();
        newTx.setCashflowPlan(plan);
        newTx.setTransactionDate(txDate);
        newTx.setAmount(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
        newTx.setCategory(item.getCategory());

        plan.getBaselineTransactions().add(newTx);
    }
}
