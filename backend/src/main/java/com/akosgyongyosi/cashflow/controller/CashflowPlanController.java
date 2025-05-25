package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.CreatePlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.ScenarioPlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.service.CashflowPlanService;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashflow-plans")
public class CashflowPlanController {

    private final CashflowPlanService planService;
    private final KpiCalculationService kpiService;

    public CashflowPlanController(
            CashflowPlanService planService,
            KpiCalculationService kpiService
    ) {
        this.planService = planService;
        this.kpiService = kpiService;
    }

    @PostMapping("/for-current-year")
    public ResponseEntity<CashflowPlan> createPlanForCurrentYear(@RequestParam String planName) {
        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end   = LocalDate.now().withDayOfYear(start.lengthOfYear());
        String groupKey = UUID.randomUUID().toString();
        CashflowPlan plan = planService.createPlanForInterval(planName, start, end, groupKey);
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/for-interval")
    public ResponseEntity<CashflowPlan> createPlanForInterval(
            @RequestBody CreatePlanRequestDTO request
    ) {
        LocalDate start = request.getStartDate();
        LocalDate end   = request.getEndDate();
        if (request.getPlanName() == null || request.getPlanName().isBlank() || start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }
        String groupKey = UUID.randomUUID().toString();
        CashflowPlan plan = planService.createPlanForInterval(
                request.getPlanName(), start, end, groupKey
        );
        return ResponseEntity.ok(plan);
    }

    @GetMapping
    public ResponseEntity<List<CashflowPlan>> getAllPlans() {
        return ResponseEntity.ok(planService.findAll());
    }

    @GetMapping("/{planId}")
    public ResponseEntity<CashflowPlan> getPlan(@PathVariable Long planId) {
        return planService.getPlan(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/group/{groupKey}/plans")
    public ResponseEntity<List<CashflowPlan>> getPlansForGroup(@PathVariable String groupKey) {
        List<CashflowPlan> plans = planService.findAllByGroupKey(groupKey);
        if (plans.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(plans);
    }

    @PostMapping("/scenarios")
    public ResponseEntity<List<CashflowPlan>> createScenarioPlans(
            @RequestBody ScenarioPlanRequestDTO request
    ) {
        LocalDate start = request.getStartDate();
        LocalDate end   = request.getEndDate();
        if (start.isAfter(end)) return ResponseEntity.badRequest().build();
        BigDecimal startingBalance = request.getStartBalance() != null
                ? request.getStartBalance()
                : BigDecimal.ZERO;
        List<CashflowPlan> threePlans = planService.createAllScenarioPlans(
                request.getBasePlanName(),
                start, end, startingBalance
        );
        return ResponseEntity.ok(threePlans);
    }

    @GetMapping("/{planId}/monthly-kpi")
    public ResponseEntity<List<MonthlyKpiDTO>> getMonthlyKpi(
            @PathVariable Long planId
    ) {
        var dashboard = kpiService.calculateForPlan(planId);
        if (dashboard == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dashboard.getMonthlyData());
    }

    @DeleteMapping("/group/{groupKey}")
    public ResponseEntity<Void> deletePlanGroup(@PathVariable String groupKey) {
        boolean deleted = planService.deletePlanGroup(groupKey);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
