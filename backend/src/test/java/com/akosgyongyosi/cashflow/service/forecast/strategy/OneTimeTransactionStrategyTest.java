package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class OneTimeTransactionStrategyTest {

    private OneTimeTransactionStrategy strat;
    private CashflowPlan plan;
    private TransactionCategory cat;

    @BeforeEach
    void setUp() {
        strat = new OneTimeTransactionStrategy();
        plan = new CashflowPlan();
        plan.setStartDate(LocalDate.of(2025, 6, 1));
        cat = new TransactionCategory();
        cat.setName("Y");
    }

    @Test
    void supports_onlyOneTime() {
        assertThat(strat.supports(LineItemType.ONE_TIME)).isTrue();
        assertThat(strat.supports(LineItemType.RECURRING)).isFalse();
    }

    @Test
    void applyForecast_usesItemDateOrPlanStart() {
        PlanLineItem a = new PlanLineItem();
        a.setType(LineItemType.ONE_TIME);
        a.setCategory(cat);
        a.setAmount(BigDecimal.valueOf(50));
        a.setTransactionDate(LocalDate.of(2025, 6, 5));
        strat.applyForecast(plan, a);

        PlanLineItem b = new PlanLineItem();
        b.setType(LineItemType.ONE_TIME);
        b.setCategory(cat);
        b.setAmount(null);
        b.setTransactionDate(null);
        strat.applyForecast(plan, b);

        assertThat(plan.getBaselineTransactions())
            .extracting(HistoricalTransaction::getTransactionDate)
            .containsExactly(LocalDate.of(2025, 6, 5), LocalDate.of(2025, 6, 1));
        assertThat(plan.getBaselineTransactions().get(1).getAmount())
            .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
