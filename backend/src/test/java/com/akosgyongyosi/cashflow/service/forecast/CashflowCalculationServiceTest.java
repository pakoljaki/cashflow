package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowCalculationServiceTest {

    /*@Test
    void applyAllAssumptions_onlyInvokesSupportedStrategy() {
        ForecastStrategy yes = new ForecastStrategy() {
            @Override public boolean supports(LineItemType type) { return true; }
            @Override public void applyForecast(CashflowPlan plan, PlanLineItem item) {
                plan.getBaselineTransactions().add(null);
            }
        };
        ForecastStrategy no = new ForecastStrategy() {
            @Override public boolean supports(LineItemType type) { return false; }
            @Override public void applyForecast(CashflowPlan plan, PlanLineItem item) {
                throw new IllegalStateException();
            }
        };

        CashflowCalculationService svc = new CashflowCalculationService(List.of(yes, no));
        CashflowPlan plan = new CashflowPlan();
        plan.getLineItems().add(new PlanLineItem());
        svc.applyAllAssumptions(plan);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
    }*/
}
