package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.CreatePlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.ScenarioPlanRequestDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.service.CashflowPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cashflow-plans")
public class CashflowPlanController {

    private final CashflowPlanService planService;

    public CashflowPlanController(CashflowPlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/for-current-year") 
    public ResponseEntity<CashflowPlan> createPlanForCurrentYear(@RequestParam String planName) {

        LocalDate currentYearStart = LocalDate.now().withDayOfYear(1);
        LocalDate currentYearEnd = LocalDate.now().withDayOfYear(currentYearStart.lengthOfYear());

        CashflowPlan plan = planService.createPlanForInterval(planName, currentYearStart, currentYearEnd);

        return ResponseEntity.ok(plan);
    }


    @PostMapping("/for-interval")
    public ResponseEntity<CashflowPlan> createPlanForInterval(@RequestBody CreatePlanRequestDTO request) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();

        if (request.getPlanName() == null || request.getPlanName().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().body(null);
        }

        CashflowPlan plan = planService.createPlanForInterval(request.getPlanName(), start, end);
        return ResponseEntity.ok(plan);
    }


    @GetMapping("/{planId}")
    public ResponseEntity<CashflowPlan> getPlan(@PathVariable Long planId) {
        return planService.getPlan(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Accepts the plan name, start date, end date, and startBalance,
     * then creates 3 scenario plans (Worst, Realistic, Best).
     *
     * Example request body JSON:
     * {
     *   "basePlanName": "MyAnnualPlan",
     *   "startDate": "2025-01-01",
     *   "endDate": "2025-12-31",
     *   "startBalance": 10000
     * }
     */
    @PostMapping("/scenarios")
    public ResponseEntity<List<CashflowPlan>> createScenarioPlans(@RequestBody ScenarioPlanRequestDTO request) {
        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        BigDecimal startingBalance = request.getStartBalance() != null
                                     ? request.getStartBalance()
                                     : BigDecimal.ZERO;

        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().build();
        }

        List<CashflowPlan> threePlans = planService.createAllScenarioPlans(
                request.getBasePlanName(), 
                start, 
                end, 
                startingBalance
        );

        return ResponseEntity.ok(threePlans);
    }
}
