package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.CreatePlanRequestDTO;
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
}
