package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO; // we'll create this DTO below
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cashflow-plans")
public class PlanLineItemController {

    private final CashflowPlanRepository planRepository;
    private final PlanLineItemRepository lineItemRepository;

    public PlanLineItemController(CashflowPlanRepository planRepository,
                                  PlanLineItemRepository lineItemRepository) {
        this.planRepository = planRepository;
        this.lineItemRepository = lineItemRepository;
    }

    // POST /api/cashflow-plans/{planId}/line-items
    @PostMapping("/{planId}/line-items")
    public ResponseEntity<PlanLineItem> createLineItem(@PathVariable Long planId,
                                                       @RequestBody PlanLineItemRequestDTO dto) {
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

        PlanLineItem lineItem = new PlanLineItem();
        lineItem.setPlan(plan);
        lineItem.setTitle(dto.getTitle());
        lineItem.setType(dto.getType());
        lineItem.setAmount(dto.getAmount());
        lineItem.setFrequency(dto.getFrequency());
        lineItem.setStartWeek(dto.getStartWeek());
        lineItem.setEndWeek(dto.getEndWeek());
        lineItem.setPercentChange(dto.getPercentChange());

        PlanLineItem saved = lineItemRepository.save(lineItem);
        return ResponseEntity.ok(saved);
    }

    // GET /api/cashflow-plans/{planId}/line-items
    @GetMapping("/{planId}/line-items")
    public ResponseEntity<List<PlanLineItem>> getLineItemsForPlan(@PathVariable Long planId) {
        List<PlanLineItem> items = lineItemRepository.findByPlanId(planId);
        return ResponseEntity.ok(items);
    }

    // PUT /api/cashflow-plans/{planId}/line-items/{itemId}
    @PutMapping("/{planId}/line-items/{itemId}")
    public ResponseEntity<PlanLineItem> updateLineItem(@PathVariable Long planId,
                                                       @PathVariable Long itemId,
                                                       @RequestBody PlanLineItemRequestDTO dto) {
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

        PlanLineItem existingItem = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("LineItem not found with ID: " + itemId));

        if (!existingItem.getPlan().getId().equals(plan.getId())) {
            return ResponseEntity.status(403).build(); // or an exception could be thrown
        }

        existingItem.setTitle(dto.getTitle());
        existingItem.setType(dto.getType());
        existingItem.setAmount(dto.getAmount());
        existingItem.setFrequency(dto.getFrequency());
        existingItem.setStartWeek(dto.getStartWeek());
        existingItem.setEndWeek(dto.getEndWeek());
        existingItem.setPercentChange(dto.getPercentChange());
    
        PlanLineItem updated = lineItemRepository.save(existingItem);

        return ResponseEntity.ok(updated);
    }

    // DELETE /api/cashflow-plans/{planId}/line-items/{itemId}
    @DeleteMapping("/{planId}/line-items/{itemId}")
    public ResponseEntity<Void> deleteLineItem(@PathVariable Long planId,
                                            @PathVariable Long itemId) {

        CashflowPlan plan = planRepository.findById(planId) // find the plan
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

        PlanLineItem item = lineItemRepository.findById(itemId) // find the lineItem
                .orElseThrow(() -> new RuntimeException("LineItem not found with ID: " + itemId));

        if (!item.getPlan().getId().equals(plan.getId())) {
            return ResponseEntity.status(403).build(); // or throw an exception
        }

        lineItemRepository.delete(item);

        return ResponseEntity.noContent().build(); //return 204 or 200
    }


}
