package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main service to calculate cashflow forecasts by applying different strategies.
 */
@Service
public class CashflowCalculationService {

    private final List<ForecastStrategy> forecastStrategies;

    @Autowired
    public CashflowCalculationService(List<ForecastStrategy> forecastStrategies) {
        this.forecastStrategies = forecastStrategies;
    }

    /**
     * Generates a weekly forecast based on a cashflow plan.
     *
     * @param plan The cashflow plan containing assumptions.
     * @return A map where the key is the week number and the value is the total amount.
     */
    public Map<Integer, BigDecimal> calculateWeeklyTotals(CashflowPlan plan) {
        Map<Integer, BigDecimal> weekTotals = new HashMap<>();

        // Initialize the weekly totals map
        long totalWeeks = ChronoUnit.WEEKS.between(plan.getStartDate(), plan.getEndDate()) + 1;
        for (int week = 1; week <= totalWeeks; week++) {
            weekTotals.put(week, BigDecimal.ZERO);
        }

        // Apply all forecast strategies to modify weekTotals
        for (ForecastStrategy strategy : forecastStrategies) {
            strategy.applyForecast(weekTotals, plan);
        }

        return weekTotals;
    }
}
