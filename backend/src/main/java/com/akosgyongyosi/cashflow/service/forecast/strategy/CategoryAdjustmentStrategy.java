package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class CategoryAdjustmentStrategy implements ForecastStrategy {

    @Override
    public boolean supports(LineItemType type) {
        return type == LineItemType.CATEGORY_ADJUSTMENT;
    }

    @Override
    public void applyForecast(CashflowPlan plan, PlanLineItem item) {
        BigDecimal factor = BigDecimal.valueOf(item.getPercentChange());
        LocalDate startDate = item.getStartDate() != null ? item.getStartDate() : plan.getStartDate();
        LocalDate endDate   = item.getEndDate()   != null ? item.getEndDate()   : plan.getEndDate();

        for (HistoricalTransaction tx : plan.getBaselineTransactions()) {
            if (tx.getCategory() != null && tx.getCategory().equals(item.getCategory())) {
                LocalDate txDate = tx.getTransactionDate();
                if (!txDate.isBefore(startDate) && !txDate.isAfter(endDate)) {
                    BigDecimal adjusted = tx.getAmount().multiply(factor);
                    tx.setAmount(adjusted);
                }
            }
        }
    }
}
