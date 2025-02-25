package com.akosgyongyosi.cashflow.service.forecast;

import java.math.BigDecimal;
import java.util.Map;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;

/**
 * Interface for all forecast strategies.
 * Each implementation will modify the weekly forecast totals differently.
 */
public interface ForecastStrategy {
    void applyForecast(Map<Integer, BigDecimal> weekTotals, CashflowPlan plan);
}
