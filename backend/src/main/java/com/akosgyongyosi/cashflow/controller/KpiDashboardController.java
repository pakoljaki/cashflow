package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kpi")
public class KpiDashboardController {

    private final KpiCalculationService kpiService;
    private final CashflowPlanRepository planRepository;
    private final CashflowCalculationService cashflowCalculationService;

    @Autowired
    public KpiDashboardController(
            KpiCalculationService kpiService,
            CashflowPlanRepository planRepository,
            CashflowCalculationService cashflowCalculationService
    ) {
        this.kpiService = kpiService;
        this.planRepository = planRepository;
        this.cashflowCalculationService = cashflowCalculationService;
    }

    @GetMapping
    public KpiDashboardDTO getKpi(@RequestParam Long planId) {
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));
        cashflowCalculationService.applyAllAssumptions(plan);
        planRepository.save(plan);
        return kpiService.calculateForPlan(planId);
    }
}
