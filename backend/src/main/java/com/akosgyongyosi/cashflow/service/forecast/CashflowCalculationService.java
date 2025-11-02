package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.fx.FxRequestCache;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import com.akosgyongyosi.cashflow.service.fx.PlanCurrencyResolver;
import com.akosgyongyosi.cashflow.service.fx.FxConversionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CashflowCalculationService {

    private final List<ForecastStrategy> forecastStrategies;
    private final FxService fxService;

    @Autowired
    public CashflowCalculationService(List<ForecastStrategy> forecastStrategies,
                                      FxService fxService) {
        this.forecastStrategies = forecastStrategies;
        this.fxService = fxService;
    }

    public void applyAllAssumptions(CashflowPlan plan) {
        Currency base = PlanCurrencyResolver.resolve(plan);
        FxRequestCache cache = new FxRequestCache(fxService);
        FxConversionContext.open(base, cache);
        try {
            for (PlanLineItem item : plan.getLineItems()) {
                if (item.getType() == LineItemType.CATEGORY_ADJUSTMENT) {
                    for (ForecastStrategy strategy : forecastStrategies) {
                        if (strategy.supports(item.getType())) {
                            strategy.applyForecast(plan, item);
                        }
                    }
                }
            }
            for (PlanLineItem item : plan.getLineItems()) {
                if (item.getType() != LineItemType.CATEGORY_ADJUSTMENT) {
                    for (ForecastStrategy strategy : forecastStrategies) {
                        if (strategy.supports(item.getType())) {
                            strategy.applyForecast(plan, item);
                        }
                    }
                }
            }
        } finally {
            FxConversionContext.close();
        }
    }
}
