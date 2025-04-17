package com.akosgyongyosi.cashflow.controller;
import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.service.kpi.KpiDashboardCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/kpi")
public class KpiDashboardController {
    private final KpiDashboardCalculationService kpiService;
    @Autowired
    public KpiDashboardController(KpiDashboardCalculationService kpiService) { this.kpiService = kpiService; }
    @GetMapping
    public KpiDashboardDTO getKpi(@RequestParam Long planId) { return kpiService.calculateKpi(planId); }
}
