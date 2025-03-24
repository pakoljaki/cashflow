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
import java.util.UUID;

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

        String groupKey = UUID.randomUUID().toString();
        CashflowPlan plan = planService.createPlanForInterval(planName, currentYearStart, currentYearEnd, groupKey);

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

        String groupKey = UUID.randomUUID().toString();
        CashflowPlan plan = planService.createPlanForInterval(request.getPlanName(), start, end, groupKey);
        return ResponseEntity.ok(plan);
    }

    @GetMapping
    public ResponseEntity<List<CashflowPlan>> getAllPlans() {
        System.out.println("✅ Reached getAllPlans()");
        List<CashflowPlan> plans = planService.findAll();
        return ResponseEntity.ok(plans);
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
        if (plans.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plans);
    }

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


