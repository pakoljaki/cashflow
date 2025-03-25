package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemResponseDTO;
import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.service.AssumptionIdGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/cashflow-plans")
public class PlanLineItemController {

    private final CashflowPlanRepository planRepository;
    private final PlanLineItemRepository lineItemRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final AssumptionIdGeneratorService assumptionIdGenService;

    public PlanLineItemController(CashflowPlanRepository planRepository,
                                  PlanLineItemRepository lineItemRepository,
                                  TransactionCategoryRepository categoryRepository,
                                  AssumptionIdGeneratorService assumptionIdGenService) {
        this.planRepository = planRepository;
        this.lineItemRepository = lineItemRepository;
        this.categoryRepository = categoryRepository;
        this.assumptionIdGenService = assumptionIdGenService;
    }

    @PostMapping("/{planId}/line-items")
    public ResponseEntity<?> createLineItem(@PathVariable Long planId,
                                            @RequestBody PlanLineItemRequestDTO dto) {
        try {
            // 1) Look up the plan
            CashflowPlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

            // 2) Build the entity
            PlanLineItem lineItem = new PlanLineItem();
            lineItem.setPlan(plan);
            lineItem.setTitle(dto.getTitle());
            lineItem.setType(dto.getType());

            // 3) Choose field behavior based on type
            switch (dto.getType()) {
                case ONE_TIME:
                    if (dto.getTransactionDate() == null) {
                        throw new RuntimeException("transactionDate is required for ONE_TIME items.");
                    }
                    lineItem.setTransactionDate(dto.getTransactionDate());
                    lineItem.setAmount(dto.getAmount());
                    lineItem.setStartDate(null);
                    lineItem.setEndDate(null);
                    lineItem.setPercentChange(null);
                    lineItem.setFrequency(Frequency.ONE_TIME);
                    break;

                case RECURRING:
                    if (dto.getFrequency() == null || dto.getFrequency() == Frequency.ONE_TIME) {
                        throw new RuntimeException("Recurring item must have a valid frequency (not ONE_TIME).");
                    }
                    lineItem.setFrequency(dto.getFrequency());
                    lineItem.setStartDate(dto.getStartDate());
                    lineItem.setEndDate(dto.getEndDate());
                    lineItem.setAmount(dto.getAmount());
                    lineItem.setTransactionDate(null);
                    lineItem.setPercentChange(null);
                    break;

                case CATEGORY_ADJUSTMENT:
                    if (dto.getPercentChange() == null) {
                        throw new RuntimeException("percentChange is required for CATEGORY_ADJUSTMENT");
                    }
                    lineItem.setFrequency(null);
                    lineItem.setTransactionDate(null);
                    lineItem.setAmount(null);
                    lineItem.setPercentChange(dto.getPercentChange());
                    lineItem.setStartDate(dto.getStartDate());
                    lineItem.setEndDate(dto.getEndDate());
                    break;

                default:
                    throw new RuntimeException("Unsupported LineItemType: " + dto.getType());
            }

            if (dto.getCategoryId() != null) {
                Optional<TransactionCategory> catOpt = categoryRepository.findById(dto.getCategoryId());
                catOpt.ifPresent(lineItem::setCategory);
            }

            if (dto.getAssumptionId() == null) {
                Long newId = assumptionIdGenService.getNextAssumptionId();
                lineItem.setAssumptionId(newId);
            } else {
                lineItem.setAssumptionId(dto.getAssumptionId());
            }

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
        dto.setAssumptionId(item.getAssumptionId());
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
