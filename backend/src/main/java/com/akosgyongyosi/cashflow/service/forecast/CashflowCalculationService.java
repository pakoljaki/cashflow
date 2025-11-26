package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.fx.FxRequestCache;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import com.akosgyongyosi.cashflow.service.fx.PlanCurrencyResolver;
import com.akosgyongyosi.cashflow.service.fx.FxConversionContext;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Service
@Slf4j
public class CashflowCalculationService {

    private final List<ForecastStrategy> forecastStrategies;
    private final FxService fxService;

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
            // Category adjustments first
            for (PlanLineItem item : plan.getLineItems()) {
                if (item.getType() == LineItemType.CATEGORY_ADJUSTMENT) {
                    applyItemWithResilience(plan, item);
                }
            }
            // Others
            for (PlanLineItem item : plan.getLineItems()) {
                if (item.getType() != LineItemType.CATEGORY_ADJUSTMENT) {
                    applyItemWithResilience(plan, item);
                }
            }
        } finally {
            FxConversionContext.close();
        }
    }

    private void applyItemWithResilience(CashflowPlan plan, PlanLineItem item) {
        for (ForecastStrategy strategy : forecastStrategies) {
            if (strategy.supports(item.getType())) {
                try {
                    strategy.applyForecast(plan, item);
                } catch (Exception ex) {
                    log.error("[ASSUMPTION-APPLY-ERROR] planId={} itemId={} type={} assumptionId={} message={}",
                            plan.getId(), item.getId(), item.getType(), item.getAssumptionId(), ex.getMessage(), ex);
                    // Do not rethrow: continue applying remaining items
                }
            }
        }
    }
}
