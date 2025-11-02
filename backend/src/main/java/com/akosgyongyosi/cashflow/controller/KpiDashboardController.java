package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiDisplayCurrencyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kpi")
public class KpiDashboardController {

    private final KpiCalculationService kpiService;
    private final CashflowPlanRepository planRepository;
    private final CashflowCalculationService cashflowCalculationService;
    private final KpiDisplayCurrencyConverter displayConverter;

    @Autowired
    public KpiDashboardController(KpiCalculationService kpiService,
                                  CashflowPlanRepository planRepository,
                                  CashflowCalculationService cashflowCalculationService,
                                  KpiDisplayCurrencyConverter displayConverter) {
        this.kpiService = kpiService;
        this.planRepository = planRepository;
        this.cashflowCalculationService = cashflowCalculationService;
        this.displayConverter = displayConverter;
    }

    @GetMapping
    public KpiDashboardDTO getKpi(@RequestParam Long planId,
                                  @RequestParam(required = false) Currency displayCurrency) {
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        cashflowCalculationService.applyAllAssumptions(plan);
        planRepository.save(plan);

        KpiDashboardDTO dash = kpiService.calculateForPlan(planId);
        Currency base = plan.getBaseCurrency();
        if (displayCurrency != null && displayCurrency != base) {
            dash = displayConverter.toDisplayCurrency(dash, plan.getStartDate(), base, displayCurrency);
        }
        return dash;
    }
}
