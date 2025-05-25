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

    /*@Test
    void supports_onlyCategoryAdjustment() {
        assertThat(strat.supports(LineItemType.CATEGORY_ADJUSTMENT)).isTrue();
        assertThat(strat.supports(LineItemType.ONE_TIME)).isFalse();
    }

    @Test
    void applyForecast_adjustsMatchingTransactions() {
        HistoricalTransaction t1 = new HistoricalTransaction();
        t1.setCategory(cat);
        t1.setTransactionDate(plan.getStartDate());
        t1.setAmount(BigDecimal.valueOf(100));
        plan.getBaselineTransactions().add(t1);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(0.10);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions())
            .first()
            .extracting(HistoricalTransaction::getAmount)
            .isEqualTo(BigDecimal.valueOf(110.0));
    }

    @Test
    void applyForecast_leavesOthersUnchanged() {
        HistoricalTransaction t2 = new HistoricalTransaction();
        t2.setCategory(cat);
        t2.setTransactionDate(LocalDate.of(2025, 4, 30));
        t2.setAmount(BigDecimal.valueOf(200));
        plan.getBaselineTransactions().add(t2);

        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.CATEGORY_ADJUSTMENT);
        item.setCategory(cat);
        item.setPercentChange(0.50);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions().get(0).getAmount())
            .isEqualTo(BigDecimal.valueOf(200));
    }*/
}
