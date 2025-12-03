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
class RecurringTransactionStrategyTest {

    @Mock
    private FxService fxService;

    private RecurringTransactionStrategy strat;
    private CashflowPlan plan;

    @BeforeEach
    void setUp() {
        lenient().when(fxService.convert(any(BigDecimal.class), any(Currency.class), any(Currency.class), any(LocalDate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

    strat = new RecurringTransactionStrategy(fxService);
        plan = new CashflowPlan();
        plan.setStartDate(LocalDate.of(2025, 7, 1));

        FxRequestCache cache = new FxRequestCache(fxService);
        FxConversionContext.open(Currency.HUF, cache);
    }

    @AfterEach
    void tearDown() {
        FxConversionContext.close();
    }

    @Test
    void supports_shouldOnlySupportRecurringType() {
        assertThat(strat.supports(LineItemType.RECURRING)).isTrue();
        assertThat(strat.supports(LineItemType.ONE_TIME)).isFalse();
        assertThat(strat.supports(LineItemType.CATEGORY_ADJUSTMENT)).isFalse();
    }

    @Test
    void applyForecast_shouldGenerateWeeklyTransactions() {
        TransactionCategory cat = new TransactionCategory();
        cat.setName("Groceries");
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.WEEKLY);
        item.setAmount(BigDecimal.valueOf(100));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 7, 1));
        item.setEndDate(LocalDate.of(2025, 7, 15));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(3);
        assertThat(plan.getBaselineTransactions())
                .extracting(HistoricalTransaction::getTransactionDate)
                .containsExactly(
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2025, 7, 8),
                        LocalDate.of(2025, 7, 15)
                );
    }

    @Test
    void applyForecast_shouldGenerateBiWeeklyTransactions() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.BI_WEEKLY);
        item.setAmount(BigDecimal.valueOf(200));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 7, 1));
        item.setEndDate(LocalDate.of(2025, 7, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(3);
        assertThat(plan.getBaselineTransactions())
                .extracting(HistoricalTransaction::getTransactionDate)
                .containsExactly(
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2025, 7, 15),
                        LocalDate.of(2025, 7, 29)
                );
    }

    @Test
    void applyForecast_shouldGenerateMonthlyTransactions() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.MONTHLY);
        item.setAmount(BigDecimal.valueOf(500));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 15));
        item.setEndDate(LocalDate.of(2025, 4, 15));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(4);
        assertThat(plan.getBaselineTransactions())
                .extracting(HistoricalTransaction::getTransactionDate)
                .containsExactly(
                        LocalDate.of(2025, 1, 15),
                        LocalDate.of(2025, 2, 15),
                        LocalDate.of(2025, 3, 15),
                        LocalDate.of(2025, 4, 15)
                );
    }

    @Test
    void applyForecast_shouldGenerateQuarterlyTransactions() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.QUARTERLY);
        item.setAmount(BigDecimal.valueOf(3000));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 1));
        item.setEndDate(LocalDate.of(2025, 12, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(4);
        assertThat(plan.getBaselineTransactions())
                .extracting(HistoricalTransaction::getTransactionDate)
                .containsExactly(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 4, 1),
                        LocalDate.of(2025, 7, 1),
                        LocalDate.of(2025, 10, 1)
                );
    }

    @Test
    void applyForecast_shouldGenerateSemiAnnualTransactions() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.SEMI_ANNUAL);
        item.setAmount(BigDecimal.valueOf(6000));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 1));
        item.setEndDate(LocalDate.of(2025, 12, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(2);
        assertThat(plan.getBaselineTransactions())
                .extracting(HistoricalTransaction::getTransactionDate)
                .containsExactly(
                        LocalDate.of(2025, 1, 1),
                        LocalDate.of(2025, 7, 1)
                );
    }

    @Test
    void applyForecast_shouldGenerateAnnualTransaction() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.ANNUAL);
        item.setAmount(BigDecimal.valueOf(12000));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 1));
        item.setEndDate(LocalDate.of(2025, 12, 31));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(1);
        assertThat(plan.getBaselineTransactions().get(0).getTransactionDate())
                .isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void applyForecast_shouldNotApplyTwice() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.MONTHLY);
        item.setAmount(BigDecimal.valueOf(500));
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 1));
        item.setEndDate(LocalDate.of(2025, 3, 1));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);
        strat.applyForecast(plan, item); // Second call

        assertThat(plan.getBaselineTransactions()).hasSize(3);
    }

    @Test
    void applyForecast_shouldHandleNullAmount() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.MONTHLY);
        item.setAmount(null);
        item.setCurrency(Currency.HUF);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 1));
        item.setEndDate(LocalDate.of(2025, 2, 1));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(2);
        assertThat(plan.getBaselineTransactions().get(0).getAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void applyForecast_shouldPreserveOriginalAmountAndCurrency() {
        TransactionCategory cat = new TransactionCategory();
        
        PlanLineItem item = new PlanLineItem();
        item.setType(LineItemType.RECURRING);
        item.setFrequency(Frequency.MONTHLY);
        item.setAmount(BigDecimal.valueOf(500));
        item.setCurrency(Currency.EUR);
        item.setCategory(cat);
        item.setStartDate(LocalDate.of(2025, 1, 1));
        item.setEndDate(LocalDate.of(2025, 3, 1));
        item.setIsApplied(false);

        strat.applyForecast(plan, item);

        assertThat(plan.getBaselineTransactions()).hasSize(3);
        for (HistoricalTransaction tx : plan.getBaselineTransactions()) {
            assertThat(tx.getOriginalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(tx.getOriginalCurrency()).isEqualTo(Currency.EUR);
        }
    }
}
