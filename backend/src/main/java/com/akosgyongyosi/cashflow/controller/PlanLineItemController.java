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

    /**
     * Add a new assumption (PlanLineItem) to an existing plan.
     * POST /api/cashflow-plans/{planId}/line-items
     */
    @PostMapping("/{planId}/line-items")
    public ResponseEntity<PlanLineItem> createLineItem(@PathVariable Long planId,
                                                       @RequestBody PlanLineItemRequestDTO dto) {
        // 1) Find the parent plan
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

        // 2) Build a new PlanLineItem from the DTO
        PlanLineItem lineItem = new PlanLineItem();
        lineItem.setPlan(plan);
        lineItem.setTitle(dto.getTitle());
        lineItem.setType(dto.getType());
        lineItem.setAmount(dto.getAmount());
        lineItem.setFrequency(dto.getFrequency());
        lineItem.setStartWeek(dto.getStartWeek());
        lineItem.setEndWeek(dto.getEndWeek());
        lineItem.setPercentChange(dto.getPercentChange());
        // etc. (category, etc. if needed)

        // 3) Save
        PlanLineItem saved = lineItemRepository.save(lineItem);
        return ResponseEntity.ok(saved);
    }

    /**
     * Get all line items (assumptions) for a given plan.
     * GET /api/cashflow-plans/{planId}/line-items
     */
    @GetMapping("/{planId}/line-items")
    public ResponseEntity<List<PlanLineItem>> getLineItemsForPlan(@PathVariable Long planId) {
        // You can either:
        // A) Use the custom finder from the repository:
        List<PlanLineItem> items = lineItemRepository.findByPlanId(planId);

        // or
        // B) load from the plan entity (but that requires a second query or lazy loading).

        return ResponseEntity.ok(items);
    }

    @PutMapping("/{planId}/line-items/{itemId}")
    public ResponseEntity<PlanLineItem> updateLineItem(@PathVariable Long planId,
                                                       @PathVariable Long itemId,
                                                       @RequestBody PlanLineItemRequestDTO dto) {
        // 1) Find the parent plan
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

        // 2) Find the existing line item
        PlanLineItem existingItem = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("LineItem not found with ID: " + itemId));

        // 3) Optional: verify that the item actually belongs to the plan
        if (!existingItem.getPlan().getId().equals(plan.getId())) {
            return ResponseEntity.status(403).build(); // or throw an exception
        }

        // 4) Update the fields
        existingItem.setTitle(dto.getTitle());
        existingItem.setType(dto.getType());
        existingItem.setAmount(dto.getAmount());
        existingItem.setFrequency(dto.getFrequency());
        existingItem.setStartWeek(dto.getStartWeek());
        existingItem.setEndWeek(dto.getEndWeek());
        existingItem.setPercentChange(dto.getPercentChange());
        // If you have category references, set them here as well

        // 5) Save the updated line item
        PlanLineItem updated = lineItemRepository.save(existingItem);

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{planId}/line-items/{itemId}")
    public ResponseEntity<Void> deleteLineItem(@PathVariable Long planId,
                                            @PathVariable Long itemId) {

        // 1) Find the plan
        CashflowPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

        // 2) Find the line item
        PlanLineItem item = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("LineItem not found with ID: " + itemId));

        // 3) Verify that the lineItem belongs to the plan
        if (!item.getPlan().getId().equals(plan.getId())) {
            return ResponseEntity.status(403).build(); // or throw an exception
        }

        // 4) Perform the delete
        lineItemRepository.delete(item);

        // 5) Return 204 No Content or 200 OK
        return ResponseEntity.noContent().build();
    }


}
