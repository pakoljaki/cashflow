package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemResponseDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.service.AssumptionIdGeneratorService;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import com.akosgyongyosi.cashflow.service.fx.PlanCurrencyResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cashflow-plans")
public class PlanLineItemController {

    private final CashflowPlanRepository planRepository;
    private final PlanLineItemRepository lineItemRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final AssumptionIdGeneratorService assumptionIdGenService;
    private final CashflowCalculationService cashflowCalculationService;

    @Autowired
    public PlanLineItemController(
            CashflowPlanRepository planRepository,
            PlanLineItemRepository lineItemRepository,
            TransactionCategoryRepository categoryRepository,
            AssumptionIdGeneratorService assumptionIdGenService,
            CashflowCalculationService cashflowCalculationService
    ) {
        this.planRepository = planRepository;
        this.lineItemRepository = lineItemRepository;
        this.categoryRepository = categoryRepository;
        this.assumptionIdGenService = assumptionIdGenService;
        this.cashflowCalculationService = cashflowCalculationService;
    }

    @PostMapping("/{planId}/line-items")
    public ResponseEntity<?> createLineItem(
            @PathVariable Long planId,
            @RequestBody PlanLineItemRequestDTO dto
    ) {
        try {
            CashflowPlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));

            PlanLineItem lineItem = new PlanLineItem();
            lineItem.setPlan(plan);
            lineItem.setTitle(dto.getTitle());
            lineItem.setType(dto.getType());

            // currency: if not provided, default to plan's functional currency
            Currency itemCurrency = dto.getCurrency() != null ? dto.getCurrency() : PlanCurrencyResolver.resolve(plan);
            lineItem.setCurrency(itemCurrency);

            switch (dto.getType()) {
                case ONE_TIME:
                    lineItem.setTransactionDate(dto.getTransactionDate());
                    lineItem.setAmount(dto.getAmount());
                    lineItem.setFrequency(Frequency.ONE_TIME);
                    lineItem.setStartDate(null);
                    lineItem.setEndDate(null);
                    lineItem.setPercentChange(null);
                    break;
                case RECURRING:
                    lineItem.setFrequency(dto.getFrequency());
                    lineItem.setStartDate(dto.getStartDate());
                    lineItem.setEndDate(dto.getEndDate());
                    lineItem.setAmount(dto.getAmount());
                    lineItem.setTransactionDate(null);
                    lineItem.setPercentChange(null);
                    break;
                case CATEGORY_ADJUSTMENT:
                    lineItem.setFrequency(null);
                    lineItem.setStartDate(dto.getStartDate());
                    lineItem.setEndDate(dto.getEndDate());
                    lineItem.setPercentChange(dto.getPercentChange());
                    lineItem.setAmount(null);
                    lineItem.setTransactionDate(null);
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

            cashflowCalculationService.applyAllAssumptions(plan);
            planRepository.save(plan);

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

        CashflowPlan plan = item.getPlan();
        lineItemRepository.delete(item);

        cashflowCalculationService.applyAllAssumptions(plan);
        planRepository.save(plan);

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
        dto.setCurrency(item.getCurrency());
        return dto;
    }
}
