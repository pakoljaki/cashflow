package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryAdjustmentStrategyTest {

    private CategoryAdjustmentStrategy strat;
    private CashflowPlan plan;
    private TransactionCategory cat;

    @BeforeEach
    void setUp() {
        strat = new CategoryAdjustmentStrategy();
        plan = new CashflowPlan();
        plan.setStartDate(LocalDate.of(2025, 5, 1));
        cat = new TransactionCategory();
        cat.setName("X");
    }

    @Test
    void supports_shouldOnlySupportCategoryAdjustmentType() {
        assertThat(strat.supports(LineItemType.CATEGORY_ADJUSTMENT)).isTrue();
        assertThat(strat.supports(LineItemType.ONE_TIME)).isFalse();
        assertThat(strat.supports(LineItemType.RECURRING)).isFalse();
    }

    @Test
    void applyForecast_shouldAdjustMatchingTransactionsByPercentage() {
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(cat);
        tx.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx.setAmount(BigDecimal.valueOf(1000));
        plan.getBaselineTransactions().add(tx);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(1.10); // 10% increase
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(LocalDate.of(2025, 5, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1100));
        assertThat(item.getIsApplied()).isTrue();
    }

    @Test
    void applyForecast_shouldOnlyAdjustTransactionsInDateRange() {
        HistoricalTransaction tx1 = new HistoricalTransaction();
        tx1.setCategory(cat);
        tx1.setTransactionDate(LocalDate.of(2025, 4, 30)); // Before range
        tx1.setAmount(BigDecimal.valueOf(100));
        plan.getBaselineTransactions().add(tx1);

        HistoricalTransaction tx2 = new HistoricalTransaction();
        tx2.setCategory(cat);
        tx2.setTransactionDate(LocalDate.of(2025, 5, 15)); // In range
        tx2.setAmount(BigDecimal.valueOf(200));
        plan.getBaselineTransactions().add(tx2);

        HistoricalTransaction tx3 = new HistoricalTransaction();
        tx3.setCategory(cat);
        tx3.setTransactionDate(LocalDate.of(2025, 6, 1)); // After range
        tx3.setAmount(BigDecimal.valueOf(300));
        plan.getBaselineTransactions().add(tx3);

        plan.setEndDate(LocalDate.of(2025, 12, 31));

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(2.0); // Double
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(LocalDate.of(2025, 5, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx1.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(tx2.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400));
        assertThat(tx3.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
    }

    @Test
    void applyForecast_shouldNotAdjustTransactionsOfDifferentCategory() {
        TransactionCategory otherCat = new TransactionCategory();
        otherCat.setName("Y");

        HistoricalTransaction tx1 = new HistoricalTransaction();
        tx1.setCategory(cat);
        tx1.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx1.setAmount(BigDecimal.valueOf(1000));
        plan.getBaselineTransactions().add(tx1);

        HistoricalTransaction tx2 = new HistoricalTransaction();
        tx2.setCategory(otherCat);
        tx2.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx2.setAmount(BigDecimal.valueOf(2000));
        plan.getBaselineTransactions().add(tx2);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(1.50);
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(LocalDate.of(2025, 5, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx1.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(tx2.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    @Test
    void applyForecast_shouldUsePlanStartDateWhenItemStartDateIsNull() {
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(cat);
        tx.setTransactionDate(plan.getStartDate());
        tx.setAmount(BigDecimal.valueOf(500));
        plan.getBaselineTransactions().add(tx);

        plan.setEndDate(LocalDate.of(2025, 12, 31));

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(2.0);
        item.setStartDate(null);
        item.setEndDate(LocalDate.of(2025, 12, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void applyForecast_shouldUsePlanEndDateWhenItemEndDateIsNull() {
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(cat);
        tx.setTransactionDate(LocalDate.of(2025, 12, 15));
        tx.setAmount(BigDecimal.valueOf(800));
        plan.getBaselineTransactions().add(tx);

        plan.setEndDate(LocalDate.of(2025, 12, 31));

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(1.25);
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(null);
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void applyForecast_shouldNotApplyTwice() {
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(cat);
        tx.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx.setAmount(BigDecimal.valueOf(1000));
        plan.getBaselineTransactions().add(tx);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(2.0);
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(LocalDate.of(2025, 5, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);
        BigDecimal afterFirst = tx.getAmount();
        strat.applyForecast(plan, item); // Second call

        assertThat(tx.getAmount()).isEqualByComparingTo(afterFirst);
    }

    @Test
    void applyForecast_shouldHandleNegativeAdjustments() {
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(cat);
        tx.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx.setAmount(BigDecimal.valueOf(1000));
        plan.getBaselineTransactions().add(tx);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(0.75); // 25% decrease
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(LocalDate.of(2025, 5, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(750));
    }

    @Test
    void applyForecast_shouldSkipTransactionsWithNullCategory() {
        HistoricalTransaction tx1 = new HistoricalTransaction();
        tx1.setCategory(cat);
        tx1.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx1.setAmount(BigDecimal.valueOf(1000));
        plan.getBaselineTransactions().add(tx1);

        HistoricalTransaction tx2 = new HistoricalTransaction();
        tx2.setCategory(null);
        tx2.setTransactionDate(LocalDate.of(2025, 5, 15));
        tx2.setAmount(BigDecimal.valueOf(2000));
        plan.getBaselineTransactions().add(tx2);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(1.50);
        item.setStartDate(LocalDate.of(2025, 5, 1));
        item.setEndDate(LocalDate.of(2025, 5, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(tx1.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(tx2.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }
}
