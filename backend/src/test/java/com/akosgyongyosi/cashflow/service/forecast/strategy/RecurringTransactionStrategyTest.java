package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecurringTransactionStrategyTest {

    private RecurringTransactionStrategy strat;
    private CashflowPlan plan;

    @BeforeEach
    void setUp() {
        strat = new RecurringTransactionStrategy();
        plan = new CashflowPlan();
        plan.setStartDate(LocalDate.of(2025, 7, 1));
    }

    /*@Test
    void supports_onlyRecurring() {
        assertThat(strat.supports(LineItemType.RECURRING)).isTrue();
        assertThat(strat.supports(LineItemType.ONE_TIME)).isFalse();
    }

    @Test
    void applyForecast_weeklyGeneratesOneEntry() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.WEEKLY);
        item.setAmount(BigDecimal.valueOf(30));
        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions())
            .singleElement()
            .extracting(HistoricalTransaction::getTransactionDate)
            .isEqualTo(plan.getStartDate());
    }

    @Test
    void calculateRecurringDates_throwsOnUnsupported() {
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.NONE);

        assertThrows(IllegalArgumentException.class, () -> strat.applyForecast(plan, item));
    }*/
}
