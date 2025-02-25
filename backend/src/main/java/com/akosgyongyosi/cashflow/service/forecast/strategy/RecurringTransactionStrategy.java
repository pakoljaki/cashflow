package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Applies recurring transactions to the forecast.
 */
@Component
public class RecurringTransactionStrategy implements ForecastStrategy {

    @Override
    public void applyForecast(Map<Integer, BigDecimal> weekTotals, CashflowPlan plan) {
        for (PlanLineItem item : plan.getLineItems()) {
            if (item.getType() == LineItemType.RECURRING) {
                for (int week = item.getStartWeek(); week <= item.getEndWeek(); week++) {
                    weekTotals.put(week, weekTotals.getOrDefault(week, BigDecimal.ZERO).add(item.getAmount()));
                }
            }
        }
    }
}
