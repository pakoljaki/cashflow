package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Adjusts forecast values for entire categories.
 */
@Component
public class CategoryAdjustmentStrategy implements ForecastStrategy {

    @Override
    public void applyForecast(Map<Integer, BigDecimal> weekTotals, CashflowPlan plan) {
        for (PlanLineItem item : plan.getLineItems()) {
            if (item.getType() == LineItemType.CATEGORY_ADJUSTMENT) {
                for (int week = item.getStartWeek(); week <= item.getEndWeek(); week++) {
                    BigDecimal baseline = weekTotals.getOrDefault(week, BigDecimal.ZERO);
                    BigDecimal adjustedAmount = baseline.multiply(BigDecimal.valueOf(1 + item.getPercentChange()));
                    weekTotals.put(week, adjustedAmount);
                }
            }
        }
    }
}
