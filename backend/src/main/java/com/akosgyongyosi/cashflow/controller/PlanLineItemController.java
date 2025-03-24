package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemResponseDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

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

    @PostMapping("/{planId}/line-items")
    public ResponseEntity<?> createLineItem(@PathVariable Long planId,
                                            @RequestBody PlanLineItemRequestDTO dto) {
        try {
            CashflowPlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

            PlanLineItem lineItem = new PlanLineItem();
            lineItem.setPlan(plan);
            lineItem.setTitle(dto.getTitle());
            lineItem.setType(dto.getType());
            lineItem.setAmount(dto.getAmount());
            
            // Validate frequency
            if (dto.getFrequency() != null && !EnumSet.allOf(Frequency.class).contains(dto.getFrequency())) {
                return ResponseEntity.badRequest().body("Invalid frequency: " + dto.getFrequency());
            }
            
            lineItem.setFrequency(dto.getFrequency());
            lineItem.setStartDate(dto.getStartDate());
            lineItem.setEndDate(dto.getEndDate());
            lineItem.setPercentChange(dto.getPercentChange());
            lineItem.setTransactionDate(dto.getTransactionDate());

            PlanLineItem saved = lineItemRepository.save(lineItem);
            return ResponseEntity.ok(toResponseDTO(saved));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating line item: " + e.getMessage());
        }
    }


    @GetMapping("/{planId}/line-items")
    public ResponseEntity<List<PlanLineItemResponseDTO>> getLineItemsForPlan(@PathVariable Long planId) {
        List<PlanLineItemResponseDTO> items = lineItemRepository.findByPlanId(planId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/{planId}/line-items/{itemId}")
    public ResponseEntity<Void> deleteLineItem(@PathVariable Long planId, @PathVariable Long itemId) {
        PlanLineItem item = lineItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("LineItem not found with ID: " + itemId));

        if (!item.getPlan().getId().equals(planId)) {
            return ResponseEntity.status(403).build();
        }

        lineItemRepository.delete(item);
        return ResponseEntity.noContent().build();
    }

    private PlanLineItemResponseDTO toResponseDTO(PlanLineItem item) {
        PlanLineItemResponseDTO dto = new PlanLineItemResponseDTO();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setType(item.getType());
        dto.setAmount(item.getAmount());
        dto.setFrequency(item.getFrequency());
        dto.setStartDate(item.getStartDate());
        dto.setEndDate(item.getEndDate());
        dto.setTransactionDate(item.getTransactionDate());
        dto.setPercentChange(item.getPercentChange());
        dto.setCategoryName(item.getCategory() != null ? item.getCategory().getName() : null);
        return dto;
    }
}
