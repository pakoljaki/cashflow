package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.CreatePlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.ScenarioPlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.CashflowPlanService;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cashflow-plans")
public class CashflowPlanController {

    private final CashflowPlanService planService;
    private final KpiCalculationService kpiService;
    private final CashflowPlanRepository planRepository;
    private final AuditLogService auditLogService;

    public CashflowPlanController(
            CashflowPlanService planService,
            KpiCalculationService kpiService,
            CashflowPlanRepository planRepository,
            AuditLogService auditLogService
    ) {
        this.planService = planService;
        this.kpiService = kpiService;
        this.planRepository = planRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/for-current-year")
    public ResponseEntity<CashflowPlan> createPlanForCurrentYear(@RequestParam String planName,
                                                                 @RequestParam(required = false) Currency baseCurrency,
                                                                 Principal principal) {
        LocalDate start = LocalDate.now().withDayOfYear(1);
        LocalDate end   = LocalDate.now().withDayOfYear(start.lengthOfYear());
        String groupKey = UUID.randomUUID().toString();
        CashflowPlan plan = planService.createPlanForInterval(planName, start, end, groupKey);
        plan.setBaseCurrency(baseCurrency != null ? baseCurrency : Currency.HUF);
        plan = planRepository.save(plan);
        auditLogService.logAction(principal != null ? principal.getName() : "system", "CREATE_PLAN", 
            Map.of("planId", plan.getId(), "planName", planName, "groupKey", groupKey));
        return ResponseEntity.ok(plan);
    }

    @PostMapping("/for-interval")
    public ResponseEntity<CashflowPlan> createPlanForInterval(@RequestBody CreatePlanRequestDTO request, 
                                                               Principal principal) {
        LocalDate start = request.getStartDate();
        LocalDate end   = request.getEndDate();
        if (request.getPlanName() == null || request.getPlanName().isBlank() || start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }
        String groupKey = UUID.randomUUID().toString();
        CashflowPlan plan = planService.createPlanForInterval(request.getPlanName(), start, end, groupKey);
        plan.setBaseCurrency(request.getBaseCurrency() != null ? request.getBaseCurrency() : Currency.HUF);
        if (request.getStartBalance() != null) plan.setStartBalance(request.getStartBalance());
        plan = planRepository.save(plan);
        auditLogService.logAction(principal != null ? principal.getName() : "system", "CREATE_PLAN", 
            Map.of("planId", plan.getId(), "planName", request.getPlanName(), "groupKey", groupKey));
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
    public ResponseEntity<List<CashflowPlan>> createScenarioPlans(@RequestBody ScenarioPlanRequestDTO request,
                                                                   Principal principal) {
        LocalDate start = request.getStartDate();
        LocalDate end   = request.getEndDate();
        if (start.isAfter(end)) return ResponseEntity.badRequest().build();
        BigDecimal startingBalance = request.getStartBalance() != null ? request.getStartBalance() : BigDecimal.ZERO;
        Currency base = request.getBaseCurrency() != null ? request.getBaseCurrency() : Currency.HUF;

        List<CashflowPlan> threePlans = planService.createAllScenarioPlans(
                request.getBasePlanName(), start, end, startingBalance, base
        );
        threePlans.forEach(planRepository::save);
        auditLogService.logAction(principal != null ? principal.getName() : "system", "CREATE_SCENARIO_PLANS", 
            Map.of("basePlanName", request.getBasePlanName(), 
                   "groupKey", threePlans.isEmpty() ? "none" : threePlans.get(0).getGroupKey(),
                   "planCount", threePlans.size()));
        return ResponseEntity.ok(threePlans);
    }

    @GetMapping("/{planId}/monthly-kpi")
    public ResponseEntity<List<MonthlyKpiDTO>> getMonthlyKpi(@PathVariable Long planId) {
        var dashboard = kpiService.calculateForPlan(planId);
        if (dashboard == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dashboard.getMonthlyData());
    }

    @DeleteMapping("/group/{groupKey}")
    public ResponseEntity<Void> deletePlanGroup(@PathVariable String groupKey, Principal principal) {
        boolean deleted = planService.deletePlanGroup(groupKey);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.logAction(principal != null ? principal.getName() : "system", "DELETE_PLAN_GROUP", 
            Map.of("groupKey", groupKey));
        return ResponseEntity.noContent().build();
    }
}
