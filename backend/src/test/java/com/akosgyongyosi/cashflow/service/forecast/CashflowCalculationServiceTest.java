package com.akosgyongyosi.cashflow.service.forecast;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashflowCalculationServiceTest {

    @Mock
    private FxService fxService;

    @Mock
    private ForecastStrategy mockStrategy1;

    @Mock
    private ForecastStrategy mockStrategy2;

    private CashflowCalculationService service;

    @BeforeEach
    void setUp() {
        List<ForecastStrategy> strategies = Arrays.asList(mockStrategy1, mockStrategy2);
        service = new CashflowCalculationService(strategies, fxService);
    }

    @Test
    void applyAllAssumptions_shouldApplyCategoryAdjustmentsFirst() {
        CashflowPlan plan = createTestPlan();
        PlanLineItem categoryItem = createLineItem(LineItemType.CATEGORY_ADJUSTMENT);
        PlanLineItem oneTimeItem = createLineItem(LineItemType.ONE_TIME);
        plan.setLineItems(Arrays.asList(categoryItem, oneTimeItem));

        when(mockStrategy1.supports(LineItemType.CATEGORY_ADJUSTMENT)).thenReturn(true);
        when(mockStrategy1.supports(LineItemType.ONE_TIME)).thenReturn(false);
        when(mockStrategy2.supports(LineItemType.CATEGORY_ADJUSTMENT)).thenReturn(false);
        when(mockStrategy2.supports(LineItemType.ONE_TIME)).thenReturn(true);

        service.applyAllAssumptions(plan);

        verify(mockStrategy1).applyForecast(plan, categoryItem);
        verify(mockStrategy2).applyForecast(plan, oneTimeItem);
    }

    @Test
    void applyAllAssumptions_shouldApplyEachStrategyToSupportedItems() {
        CashflowPlan plan = createTestPlan();
        PlanLineItem item1 = createLineItem(LineItemType.ONE_TIME);
        PlanLineItem item2 = createLineItem(LineItemType.RECURRING);
        plan.setLineItems(Arrays.asList(item1, item2));

        when(mockStrategy1.supports(LineItemType.ONE_TIME)).thenReturn(true);
        when(mockStrategy1.supports(LineItemType.RECURRING)).thenReturn(false);
        when(mockStrategy2.supports(LineItemType.ONE_TIME)).thenReturn(false);
        when(mockStrategy2.supports(LineItemType.RECURRING)).thenReturn(true);

        service.applyAllAssumptions(plan);

        verify(mockStrategy1).applyForecast(plan, item1);
        verify(mockStrategy2).applyForecast(plan, item2);
        verify(mockStrategy1, never()).applyForecast(plan, item2);
        verify(mockStrategy2, never()).applyForecast(plan, item1);
    }

    @Test
    void applyAllAssumptions_shouldHandleEmptyLineItems() {
        CashflowPlan plan = createTestPlan();
        plan.setLineItems(new ArrayList<>());

        service.applyAllAssumptions(plan);

        verify(mockStrategy1, never()).applyForecast(any(), any());
        verify(mockStrategy2, never()).applyForecast(any(), any());
    }

    @Test
    void applyAllAssumptions_shouldHandleMultipleCategoryAdjustments() {
        CashflowPlan plan = createTestPlan();
        PlanLineItem categoryItem1 = createLineItem(LineItemType.CATEGORY_ADJUSTMENT);
        PlanLineItem categoryItem2 = createLineItem(LineItemType.CATEGORY_ADJUSTMENT);
        PlanLineItem oneTimeItem = createLineItem(LineItemType.ONE_TIME);
        plan.setLineItems(Arrays.asList(categoryItem1, categoryItem2, oneTimeItem));

        when(mockStrategy1.supports(LineItemType.CATEGORY_ADJUSTMENT)).thenReturn(true);
        when(mockStrategy1.supports(LineItemType.ONE_TIME)).thenReturn(false);
        when(mockStrategy2.supports(any(LineItemType.class))).thenReturn(false);

        service.applyAllAssumptions(plan);

        verify(mockStrategy1, times(2)).applyForecast(eq(plan), any(PlanLineItem.class));
    }

    @Test
    void applyAllAssumptions_shouldSkipItemsWhenNoStrategySupports() {
        CashflowPlan plan = createTestPlan();
        PlanLineItem item = createLineItem(LineItemType.ONE_TIME);
        plan.setLineItems(Arrays.asList(item));

        when(mockStrategy1.supports(any(LineItemType.class))).thenReturn(false);
        when(mockStrategy2.supports(any(LineItemType.class))).thenReturn(false);

        service.applyAllAssumptions(plan);

        verify(mockStrategy1, never()).applyForecast(any(), any());
        verify(mockStrategy2, never()).applyForecast(any(), any());
    }

    @Test
    void applyAllAssumptions_shouldUseBaseCurrencyFromPlan() {
        CashflowPlan plan = createTestPlan();
        plan.setBaseCurrency(Currency.EUR);
        PlanLineItem item = createLineItem(LineItemType.ONE_TIME);
        plan.setLineItems(Arrays.asList(item));

        when(mockStrategy1.supports(LineItemType.ONE_TIME)).thenReturn(true);

        service.applyAllAssumptions(plan);

        verify(mockStrategy1).applyForecast(plan, item);
    }

    @Test
    void applyAllAssumptions_shouldHandleMixedLineItemTypes() {
        CashflowPlan plan = createTestPlan();
        PlanLineItem categoryItem = createLineItem(LineItemType.CATEGORY_ADJUSTMENT);
        PlanLineItem oneTimeItem = createLineItem(LineItemType.ONE_TIME);
        PlanLineItem recurringItem = createLineItem(LineItemType.RECURRING);
        plan.setLineItems(Arrays.asList(oneTimeItem, categoryItem, recurringItem));

        when(mockStrategy1.supports(LineItemType.CATEGORY_ADJUSTMENT)).thenReturn(true);
        when(mockStrategy1.supports(LineItemType.ONE_TIME)).thenReturn(false);
        when(mockStrategy1.supports(LineItemType.RECURRING)).thenReturn(false);
        when(mockStrategy2.supports(LineItemType.CATEGORY_ADJUSTMENT)).thenReturn(false);
        when(mockStrategy2.supports(LineItemType.ONE_TIME)).thenReturn(true);
        when(mockStrategy2.supports(LineItemType.RECURRING)).thenReturn(true);

        service.applyAllAssumptions(plan);

        verify(mockStrategy1).applyForecast(plan, categoryItem);
        verify(mockStrategy2).applyForecast(plan, oneTimeItem);
        verify(mockStrategy2).applyForecast(plan, recurringItem);
    }

    private CashflowPlan createTestPlan() {
        CashflowPlan plan = new CashflowPlan();
        plan.setBaseCurrency(Currency.HUF);
        plan.setStartDate(LocalDate.of(2024, 1, 1));
        plan.setEndDate(LocalDate.of(2024, 12, 31));
        plan.setBaselineTransactions(new ArrayList<>());
        plan.setLineItems(new ArrayList<>());
        return plan;
    }

    private PlanLineItem createLineItem(LineItemType type) {
        PlanLineItem item = new PlanLineItem();
        item.setType(type);
        item.setAmount(BigDecimal.valueOf(1000));
        item.setCurrency(Currency.HUF);
        item.setIsApplied(false);
        return item;
    }
}
