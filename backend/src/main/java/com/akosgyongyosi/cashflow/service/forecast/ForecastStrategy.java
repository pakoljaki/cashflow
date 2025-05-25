package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.entity.LineItemType;

public interface ForecastStrategy {

    boolean supports(LineItemType type);

    void applyForecast(CashflowPlan plan, PlanLineItem item);
}
