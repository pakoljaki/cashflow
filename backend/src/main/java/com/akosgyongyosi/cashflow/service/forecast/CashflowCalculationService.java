package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CashflowCalculationService {

    private final List<ForecastStrategy> forecastStrategies;

    @Autowired
    public CashflowCalculationService(List<ForecastStrategy> forecastStrategies) {
        this.forecastStrategies = forecastStrategies;
    }

    public void applyAllAssumptions(CashflowPlan plan) {
        for (PlanLineItem item : plan.getLineItems()) {
            for (ForecastStrategy strategy : forecastStrategies) {
                if (strategy.supports(item.getType())) {
                    strategy.applyForecast(plan, item);
                }
            }
        }
    }
}
