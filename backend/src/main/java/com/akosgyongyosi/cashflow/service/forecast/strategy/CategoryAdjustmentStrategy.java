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
        for (HistoricalTransaction transaction : plan.getBaselineTransactions()) {
            if (transaction.getCategory() != null && transaction.getCategory().equals(item.getCategory())) {
                LocalDate txDate = transaction.getTransactionDate();
                LocalDate startDate = plan.getStartDate();
                LocalDate endDate = plan.getStartDate();

                if (!txDate.isBefore(startDate) && !txDate.isAfter(endDate)) {
                    BigDecimal adjustedAmount = transaction.getAmount().multiply(BigDecimal.valueOf(1 + item.getPercentChange()));
                    transaction.setAmount(adjustedAmount);
                }
            }
        }
    }
}
