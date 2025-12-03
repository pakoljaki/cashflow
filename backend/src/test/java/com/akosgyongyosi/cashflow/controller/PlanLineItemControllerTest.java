package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO;
import com.akosgyongyosi.cashflow.dto.PlanLineItemResponseDTO;
import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.service.AssumptionIdGeneratorService;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SuppressWarnings({"null"})
class PlanLineItemControllerTest {

    @Mock
    private CashflowPlanRepository planRepository;

    @Mock
    private PlanLineItemRepository lineItemRepository;

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @Mock
    private AssumptionIdGeneratorService assumptionIdGenService;

    @Mock
    private CashflowCalculationService cashflowCalculationService;

    @Mock
    private com.akosgyongyosi.cashflow.service.fx.RateLookupService rateLookupService;

    @Mock
    private com.akosgyongyosi.cashflow.service.fx.TransactionDateRangeFxService transactionDateRangeFxService;

    @Mock
    private com.akosgyongyosi.cashflow.service.CashflowPlanService cashflowPlanService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PlanLineItemController planLineItemController;

    private Principal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@example.com");
    }

    @Test
    void createLineItem_shouldCreateOneTimeTransaction() {
        Long planId = 1L;
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        plan.setLineItems(new java.util.ArrayList<>()); 

        PlanLineItemRequestDTO dto = new PlanLineItemRequestDTO();
        dto.setTitle("One-time expense");
        dto.setType(LineItemType.ONE_TIME);
        dto.setAmount(new BigDecimal("1000.00"));
        dto.setTransactionDate(LocalDate.of(2024, 6, 15));

        PlanLineItem savedItem = new PlanLineItem();
        savedItem.setId(1L);
        savedItem.setTitle("One-time expense");
        savedItem.setType(LineItemType.ONE_TIME);
        savedItem.setAssumptionId(100L);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(assumptionIdGenService.getNextAssumptionId()).thenReturn(200L);
        when(lineItemRepository.save(any(PlanLineItem.class))).thenReturn(savedItem);
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);

        ResponseEntity<?> response = planLineItemController.createLineItem(planId, dto, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(planRepository).findById(planId);
    verify(lineItemRepository).save(any(PlanLineItem.class));
    verify(cashflowCalculationService).applyAllAssumptions(plan);
    verify(lineItemRepository).saveAll(anyList());
    verify(planRepository).save(plan);
    }

    @Test
    void createLineItem_shouldCreateRecurringTransaction() {
        Long planId = 1L;
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        plan.setLineItems(new java.util.ArrayList<>()); 
        PlanLineItemRequestDTO dto = new PlanLineItemRequestDTO();
        dto.setTitle("Monthly salary");
        dto.setType(LineItemType.RECURRING);
        dto.setAmount(new BigDecimal("5000.00"));
        dto.setFrequency(Frequency.MONTHLY);
        dto.setStartDate(LocalDate.of(2024, 1, 1));
        dto.setEndDate(LocalDate.of(2024, 12, 31));

        PlanLineItem savedItem = new PlanLineItem();
        savedItem.setId(2L);
        savedItem.setTitle("Monthly salary");
        savedItem.setType(LineItemType.RECURRING);
        savedItem.setAssumptionId(101L); 

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(assumptionIdGenService.getNextAssumptionId()).thenReturn(101L);
        when(lineItemRepository.save(any(PlanLineItem.class))).thenReturn(savedItem);
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);

        ResponseEntity<?> response = planLineItemController.createLineItem(planId, dto, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(lineItemRepository).save(any(PlanLineItem.class));
    verify(lineItemRepository).saveAll(anyList());
    }

    @Test
    void createLineItem_shouldCreateCategoryAdjustment() {
        Long planId = 1L;
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        plan.setLineItems(new java.util.ArrayList<>());

        PlanLineItemRequestDTO dto = new PlanLineItemRequestDTO();
        dto.setTitle("Increase expenses by 10%");
        dto.setType(LineItemType.CATEGORY_ADJUSTMENT);
        dto.setPercentChange(10.0);
        dto.setStartDate(LocalDate.of(2024, 1, 1));
        dto.setEndDate(LocalDate.of(2024, 12, 31));
        dto.setCategoryId(5L);

        TransactionCategory category = new TransactionCategory();
        category.setId(5L);
        category.setName("Groceries");

        PlanLineItem savedItem = new PlanLineItem();
        savedItem.setId(3L);
        savedItem.setTitle("Increase expenses by 10%");
        savedItem.setType(LineItemType.CATEGORY_ADJUSTMENT);
        savedItem.setAssumptionId(102L);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(categoryRepository.findById(5L)).thenReturn(Optional.of(category));
        when(assumptionIdGenService.getNextAssumptionId()).thenReturn(102L);
        when(lineItemRepository.save(any(PlanLineItem.class))).thenReturn(savedItem);
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);

        ResponseEntity<?> response = planLineItemController.createLineItem(planId, dto, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(categoryRepository).findById(5L);
    verify(lineItemRepository).save(any(PlanLineItem.class));
    verify(lineItemRepository).saveAll(anyList());
    }

    @Test
    void createLineItem_shouldReturnErrorWhenPlanNotFound() {
        Long planId = 999L;
        PlanLineItemRequestDTO dto = new PlanLineItemRequestDTO();
        dto.setTitle("Test");
        dto.setType(LineItemType.ONE_TIME);

        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = planLineItemController.createLineItem(planId, dto, principal);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat((response.getBody() == null ? "" : response.getBody().toString())).contains("Error creating line item");
    verify(lineItemRepository, never()).save(any());
    }

    @Test
    void getLineItemsForPlan_shouldReturnAllLineItems() {
        Long planId = 1L;
        
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        
        PlanLineItem item1 = new PlanLineItem();
        item1.setId(1L);
        item1.setTitle("Item 1");
        item1.setType(LineItemType.ONE_TIME);

        PlanLineItem item2 = new PlanLineItem();
        item2.setId(2L);
        item2.setTitle("Item 2");
        item2.setType(LineItemType.RECURRING);

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(lineItemRepository.findByPlanId(planId)).thenReturn(Arrays.asList(item1, item2));

        ResponseEntity<List<PlanLineItemResponseDTO>> response = 
            planLineItemController.getLineItemsForPlan(planId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        verify(planRepository).findById(planId);
        verify(lineItemRepository).findByPlanId(planId);
    }

    @Test
    void deleteLineItem_shouldDeleteAndRecalculate() {
        Long planId = 1L;
        Long itemId = 10L;

        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setLineItems(new java.util.ArrayList<>());

        PlanLineItem item = new PlanLineItem();
        item.setId(itemId);
        item.setPlan(plan);

        when(lineItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        doNothing().when(lineItemRepository).delete(item);
        when(cashflowPlanService.regenerateBaseline(planId)).thenReturn(plan);
        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);

        ResponseEntity<Void> response = planLineItemController.deleteLineItem(planId, itemId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(lineItemRepository).delete(item);
        verify(cashflowPlanService).regenerateBaseline(planId);
        verify(planRepository, times(2)).findById(planId);
    verify(lineItemRepository).saveAll(anyList()); 
        verify(cashflowCalculationService).applyAllAssumptions(plan);
        verify(planRepository).save(plan);
    }

    @Test
    void deleteLineItem_shouldReturnForbiddenWhenPlanMismatch() {
        Long planId = 1L;
        Long itemId = 10L;

        CashflowPlan otherPlan = new CashflowPlan();
        otherPlan.setId(2L);

        PlanLineItem item = new PlanLineItem();
        item.setId(itemId);
        item.setPlan(otherPlan);

        when(lineItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        ResponseEntity<Void> response = planLineItemController.deleteLineItem(planId, itemId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(lineItemRepository, never()).delete(any());
        verify(cashflowCalculationService, never()).applyAllAssumptions(any());
    }
}
