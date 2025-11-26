package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.service.fx.FxConversionContext;
import com.akosgyongyosi.cashflow.service.fx.FxRequestCache;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OneTimeTransactionStrategyTest {

    @Mock
    private FxService fxService;

    private OneTimeTransactionStrategy strat;
    private CashflowPlan plan;
    private TransactionCategory cat;

    @BeforeEach
    void setUp() {
        lenient().when(fxService.convert(any(BigDecimal.class), any(Currency.class), any(Currency.class), any(LocalDate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        strat = new OneTimeTransactionStrategy();
        plan = new CashflowPlan();
        plan.setStartDate(LocalDate.of(2025, 6, 1));
        cat = new TransactionCategory();
        cat.setName("Y");

        FxRequestCache cache = new FxRequestCache(fxService);
        FxConversionContext.open(Currency.HUF, cache);
    }

    @AfterEach
    void tearDown() {
        FxConversionContext.close();
    }

    @Test
    void supports_shouldOnlySupportOneTimeType() {
        assertThat(strat.supports(LineItemType.ONE_TIME)).isTrue();
        assertThat(strat.supports(LineItemType.RECURRING)).isFalse();
        assertThat(strat.supports(LineItemType.CATEGORY_ADJUSTMENT)).isFalse();
    }

    @Test
    void applyForecast_shouldUseItemTransactionDate() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(BigDecimal.valueOf(500));
        item.setCurrency(Currency.HUF);
        item.setTransactionDate(LocalDate.of(2025, 6, 15));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
        assertThat(plan.getBaselineTransactions().get(0).getTransactionDate())
                .isEqualTo(LocalDate.of(2025, 6, 15));
        assertThat(item.getIsApplied()).isTrue();
    }

    @Test
    void applyForecast_shouldUsePlanStartDateWhenItemDateIsNull() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(BigDecimal.valueOf(300));
        item.setCurrency(Currency.HUF);
        item.setTransactionDate(null);
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
        assertThat(plan.getBaselineTransactions().get(0).getTransactionDate())
                .isEqualTo(plan.getStartDate());
    }

    @Test
    void applyForecast_shouldHandleNullAmount() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(null);
        item.setCurrency(Currency.HUF);
        item.setTransactionDate(LocalDate.of(2025, 6, 10));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
        assertThat(plan.getBaselineTransactions().get(0).getAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void applyForecast_shouldSetCorrectCategory() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(BigDecimal.valueOf(1000));
        item.setCurrency(Currency.HUF);
        item.setTransactionDate(LocalDate.of(2025, 6, 20));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
        assertThat(plan.getBaselineTransactions().get(0).getCategory()).isEqualTo(cat);
    }

    @Test
    void applyForecast_shouldNotApplyTwice() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(BigDecimal.valueOf(500));
        item.setCurrency(Currency.HUF);
        item.setTransactionDate(LocalDate.of(2025, 6, 15));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);
        strat.applyForecast(plan, item); // Second call

        assertThat(plan.getBaselineTransactions()).hasSize(1);
    }

    @Test
    void applyForecast_shouldLinkTransactionToPlan() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(BigDecimal.valueOf(750));
        item.setCurrency(Currency.HUF);
        item.setTransactionDate(LocalDate.of(2025, 6, 25));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions().get(0).getCashflowPlan()).isEqualTo(plan);
    }

    @Test
    void applyForecast_shouldPreserveOriginalAmountAndCurrency() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.ONE_TIME);
        item.setCategory(cat);
        item.setAmount(BigDecimal.valueOf(3000));
        item.setCurrency(Currency.USD);
        item.setTransactionDate(LocalDate.of(2025, 6, 30));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
        HistoricalTransaction tx = plan.getBaselineTransactions().get(0);
        assertThat(tx.getOriginalAmount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
        assertThat(tx.getOriginalCurrency()).isEqualTo(Currency.USD);
    }
}
