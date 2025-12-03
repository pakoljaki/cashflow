package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemResponseDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.service.AssumptionIdGeneratorService;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import com.akosgyongyosi.cashflow.service.fx.PlanCurrencyResolver;
import com.akosgyongyosi.cashflow.service.fx.RateLookupService;
import com.akosgyongyosi.cashflow.dto.FxWarningDTO;
import com.akosgyongyosi.cashflow.dto.RateLookupResultDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@RestController
@RequestMapping("/api/cashflow-plans")
@Slf4j
@SuppressWarnings({"java:S107","null"}) 
public class PlanLineItemController {

    private final CashflowPlanRepository planRepository;
    private final PlanLineItemRepository lineItemRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final AssumptionIdGeneratorService assumptionIdGenService;
    private final CashflowCalculationService cashflowCalculationService;
    private final RateLookupService rateLookupService;
    private final com.akosgyongyosi.cashflow.service.fx.TransactionDateRangeFxService transactionDateRangeFxService;
    private final com.akosgyongyosi.cashflow.service.CashflowPlanService cashflowPlanService;
    private final AuditLogService auditLogService;

    public PlanLineItemController(
            CashflowPlanRepository planRepository,
            PlanLineItemRepository lineItemRepository,
            TransactionCategoryRepository categoryRepository,
            AssumptionIdGeneratorService assumptionIdGenService,
            CashflowCalculationService cashflowCalculationService,
            RateLookupService rateLookupService,
            com.akosgyongyosi.cashflow.service.fx.TransactionDateRangeFxService transactionDateRangeFxService,
            com.akosgyongyosi.cashflow.service.CashflowPlanService cashflowPlanService,
            AuditLogService auditLogService
    ) {
        this.planRepository = planRepository;
        this.lineItemRepository = lineItemRepository;
        this.categoryRepository = categoryRepository;
        this.assumptionIdGenService = assumptionIdGenService;
        this.cashflowCalculationService = cashflowCalculationService;
        this.rateLookupService = rateLookupService;
        this.transactionDateRangeFxService = transactionDateRangeFxService;
        this.cashflowPlanService = cashflowPlanService;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/{planId}/line-items")
    @Transactional
    public ResponseEntity<Object> createLineItem(
        @org.springframework.lang.NonNull @PathVariable Long planId,
        @org.springframework.lang.NonNull @RequestBody PlanLineItemRequestDTO dto,
        Principal principal
    ) {
        try {
            CashflowPlan plan = planRepository.findById(planId)
                    .orElseThrow(() -> new RuntimeException("Plan not found with ID: " + planId));
        log.debug("[CREATE] planId={} type={} title={} incomingAssumptionId={}", planId, dto.getType(), dto.getTitle(), dto.getAssumptionId());

            PlanLineItem lineItem = new PlanLineItem();
            lineItem.setPlan(plan);
            lineItem.setTitle(dto.getTitle());
            lineItem.setType(dto.getType());

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
                    throw new IllegalArgumentException("Unsupported LineItemType: " + dto.getType());
            }

            if (dto.getCategoryId() != null) {
                Optional<TransactionCategory> catOpt = categoryRepository.findById(dto.getCategoryId());
                catOpt.ifPresent(lineItem::setCategory);
            }

            if (dto.getAssumptionId() == null) {
                Long reusableId = tryFindReusableAssumptionId(dto);
                if (reusableId != null) {
                    lineItem.setAssumptionId(reusableId);
                } else {
                    Long newId = assumptionIdGenService.getNextAssumptionId();
                    lineItem.setAssumptionId(newId);
                }
            } else {
                lineItem.setAssumptionId(dto.getAssumptionId());
            }
            
            PlanLineItem existingInDb = lineItemRepository.findByPlanIdAndAssumptionId(planId, lineItem.getAssumptionId());
            if (existingInDb != null) {
                log.warn("[CREATE-DUPLICATE-DB] planId={} assumptionId={} already exists in database (id={}), skipping", 
                    planId, lineItem.getAssumptionId(), existingInDb.getId());
                return ResponseEntity.ok(toResponseDTO(existingInDb, plan));
            }
            
            boolean alreadyExists = plan.getLineItems() != null && 
                plan.getLineItems().stream()
                    .anyMatch(li -> lineItem.getAssumptionId().equals(li.getAssumptionId()));
            if (alreadyExists) {
                log.warn("[CREATE-DUPLICATE-MEM] planId={} assumptionId={} already exists in memory, skipping", planId, lineItem.getAssumptionId());
                PlanLineItem existing = plan.getLineItems().stream()
                    .filter(li -> lineItem.getAssumptionId().equals(li.getAssumptionId()))
                    .findFirst()
                    .orElse(lineItem);
                return ResponseEntity.ok(toResponseDTO(existing, plan));
            }

            PlanLineItem saved = lineItemRepository.save(lineItem); 
    
            if (plan.getLineItems() == null) {
                plan.setLineItems(new java.util.ArrayList<>()); 
            }
            if (plan.getLineItems().stream().noneMatch(li -> li.getId() != null && li.getId().equals(saved.getId()))) {
                plan.getLineItems().add(saved);
            }

            long appliedBefore = plan.getLineItems().stream().filter(li -> Boolean.TRUE.equals(li.getIsApplied())).count();
            long totalBefore = plan.getLineItems().size();
            log.debug("[APPLY-BEFORE] planId={} appliedBefore={}/{}", planId, appliedBefore, totalBefore);

            ensureFxRatesForPlan(plan);

            cashflowCalculationService.applyAllAssumptions(plan);

            long appliedAfter = plan.getLineItems().stream().filter(li -> Boolean.TRUE.equals(li.getIsApplied())).count();
            long totalAfter = plan.getLineItems().size();
            log.debug("[APPLY-AFTER] planId={} newItemId={} assumptionId={} appliedAfter={}/{}", planId, saved.getId(), saved.getAssumptionId(), appliedAfter, totalAfter);

            lineItemRepository.saveAll(plan.getLineItems());
            planRepository.save(plan);
            log.debug("[CREATE-DONE] planId={} itemId={} isApplied={} baselineTxCount={}", planId, saved.getId(), saved.getIsApplied(), plan.getBaselineTransactions() == null ? -1 : plan.getBaselineTransactions().size());

            auditLogService.logAction(principal != null ? principal.getName() : "system", "CREATE_ASSUMPTION", 
                java.util.Map.of("planId", planId, "itemId", saved.getId(), 
                                 "assumptionId", saved.getAssumptionId(), 
                                 "type", dto.getType().name(), 
                                 "title", dto.getTitle()));

            return ResponseEntity.ok(toResponseDTO(saved, plan));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating line item: " + e.getMessage());
        }
    }

    private Long tryFindReusableAssumptionId(PlanLineItemRequestDTO dto) {
        try {
            PlanLineItem match = null;
            if (dto.getType() == LineItemType.ONE_TIME && dto.getTransactionDate() != null) {
                match = lineItemRepository.findFirstByTitleIgnoreCaseAndTypeAndTransactionDate(dto.getTitle(), dto.getType(), dto.getTransactionDate());
            } else if (dto.getType() == LineItemType.RECURRING && dto.getStartDate() != null && dto.getFrequency() != null) {
                match = lineItemRepository.findFirstByTitleIgnoreCaseAndTypeAndStartDateAndFrequency(dto.getTitle(), dto.getType(), dto.getStartDate(), dto.getFrequency());
            } else if (dto.getType() == LineItemType.CATEGORY_ADJUSTMENT && dto.getStartDate() != null && dto.getPercentChange() != null) {
                match = lineItemRepository.findFirstByTitleIgnoreCaseAndTypeAndStartDateAndPercentChange(dto.getTitle(), dto.getType(), dto.getStartDate(), dto.getPercentChange());
            }
            return match != null ? match.getAssumptionId() : null;
        } catch (Exception ex) {
            log.debug("[ASSUMPTION-ID-REUSE] signature search failed: {}", ex.getMessage());
            return null;
        }
    }

    @GetMapping("/{planId}/line-items")
    public ResponseEntity<List<PlanLineItemResponseDTO>> getLineItemsForPlan(@org.springframework.lang.NonNull @PathVariable Long planId) {
        Optional<CashflowPlan> planOpt = planRepository.findById(planId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        CashflowPlan plan = planOpt.get();
        
    List<PlanLineItemResponseDTO> items = lineItemRepository.findByPlanId(planId).stream()
        .map(item -> toResponseDTO(item, plan))
        .toList();
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/{planId}/line-items/{itemId}")
    public ResponseEntity<Void> deleteLineItem(@org.springframework.lang.NonNull @PathVariable Long planId, @org.springframework.lang.NonNull @PathVariable Long itemId) {
        Optional<PlanLineItem> itemOpt = lineItemRepository.findById(itemId);
        if (itemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        PlanLineItem item = itemOpt.get();
        if (!item.getPlan().getId().equals(planId)) {
            return ResponseEntity.status(403).build();
        }

        CashflowPlan plan = item.getPlan();
        
        lineItemRepository.delete(item);
        
        cashflowPlanService.regenerateBaseline(plan.getId());
        
        plan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        
        if (plan.getLineItems() != null) {
            for (PlanLineItem remainingItem : plan.getLineItems()) {
                remainingItem.setIsApplied(false);
            }
            lineItemRepository.saveAll(plan.getLineItems());
        }
        
        plan = planRepository.findById(plan.getId())
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        
        ensureFxRatesForPlan(plan);
        cashflowCalculationService.applyAllAssumptions(plan);
        planRepository.save(plan);

        return ResponseEntity.noContent().build();
    }

    private PlanLineItemResponseDTO toResponseDTO(PlanLineItem item, CashflowPlan plan) {
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
        
        Currency planBaseCurrency = PlanCurrencyResolver.resolve(plan);
        if (item.getCurrency() != null && item.getCurrency() != planBaseCurrency) {
            LocalDate txDate = item.getTransactionDate() != null ? item.getTransactionDate() : item.getStartDate();
            if (txDate != null) {
                try {
                    RateLookupResultDTO res = rateLookupService.lookup(item.getCurrency(), planBaseCurrency, txDate);
                    dto.setWarnings(res.getWarnings());
                    dto.setRateMeta(res.getMeta());
                    if (!res.getWarnings().isEmpty()) {
                        FxWarningDTO first = res.getWarnings().get(0);
                        dto.setWarning(first.getMessage());
                    } else {
                        dto.setWarning(String.format("Currency conversion: %s → %s rate date %s", item.getCurrency(), planBaseCurrency, res.getMeta().getRateDateUsed()));
                    }
                } catch (Exception ex) {
                    dto.setWarning("FX lookup failed: " + ex.getMessage());
                }
            }
        }
        
        return dto;
    }
    
    private void ensureFxRatesForPlan(CashflowPlan plan) {
        try {
            java.util.Set<LocalDate> allDates = new java.util.HashSet<>();
            
            for (PlanLineItem item : plan.getLineItems()) {
                if (item.getTransactionDate() != null) {
                    allDates.add(item.getTransactionDate());
                } else if (item.getStartDate() != null && item.getEndDate() != null) {
                    allDates.add(item.getStartDate());
                    allDates.add(item.getEndDate());
                }
            }
            
            if (!allDates.isEmpty()) {
                transactionDateRangeFxService.ensureRatesForTransactionsWithForwardCoverage(allDates);
            }
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                .warn("Failed to pre-fetch FX rates for plan {}: {}", plan.getId(), ex.getMessage());
        }
    }
}
