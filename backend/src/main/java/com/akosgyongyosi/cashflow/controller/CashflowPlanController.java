package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.service.CashflowPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/cashflow-plans")
public class CashflowPlanController {

    private final CashflowPlanService planService;

    public CashflowPlanController(CashflowPlanService planService) {
        this.planService = planService;
    }

    /**
     * Create a new cashflow plan from last year's transactions.
     * Example request:
     * POST /api/cashflow-plans/from-last-year?planName=2025%20Plan&startDate=2025-01-01&endDate=2025-12-31
     */
    @PostMapping("/from-last-year")
    public ResponseEntity<CashflowPlan> createPlanFromLastYear(
            @RequestParam String planName,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        CashflowPlan plan = planService.createPlanFromLastYear(planName, start, end);
        return ResponseEntity.ok(plan);
    }

    /**
     * Create a new cashflow plan manually.
     */
    @PostMapping
    public ResponseEntity<CashflowPlan> createPlan(@RequestBody CashflowPlan plan) {
        CashflowPlan saved = planService.createPlan(plan);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get a cashflow plan by ID.
     */
    @GetMapping("/{planId}")
    public ResponseEntity<CashflowPlan> getPlan(@PathVariable Long planId) {
        return planService.getPlan(planId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
