package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.HistoricalTransactionRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class KpiCalculationServiceTest {

    private TransactionRepository txRepo;
    private HistoricalTransactionRepository histRepo;
    private CashflowPlanRepository planRepo;
    private FxService fxService;
    private KpiCalculationService service;

    @BeforeEach
    void setUp() {
        txRepo = mock(TransactionRepository.class);
        histRepo = mock(HistoricalTransactionRepository.class);
        planRepo = mock(CashflowPlanRepository.class);
        fxService = mock(FxService.class);
        service = new KpiCalculationService(txRepo, histRepo, planRepo, fxService);
    }

    @Test
    void calculateForPeriod_no_transactions_returns_zero_activity() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        when(txRepo.findByBookingDateBetween(start, end)).thenReturn(List.of());

        KpiDashboardDTO result = service.calculateForPeriod(start, end, BigDecimal.ZERO);

        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStartBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMonthlyData()).hasSize(12);
    }

    @Test
    void calculateForPeriod_single_positive_transaction_adds_to_income() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        Transaction tx = createTransaction(date, BigDecimal.valueOf(1000), TransactionDirection.POSITIVE, Currency.HUF);
        when(txRepo.findByBookingDateBetween(any(), any())).thenReturn(List.of(tx));

        KpiDashboardDTO result = service.calculateForPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                BigDecimal.ZERO,
                Currency.HUF);

        assertThat(result.getTotalRevenue()).isEqualByComparingTo("1000");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        MonthlyKpiDTO june = result.getMonthlyData().get(5); // Month 6 at index 5
        assertThat(june.getTotalIncome()).isEqualByComparingTo("1000");
        assertThat(june.getBankBalance()).isEqualByComparingTo("1000");
    }

    @Test
    void calculateForPeriod_single_negative_transaction_adds_to_expense() {
        LocalDate date = LocalDate.of(2024, 3, 10);
        Transaction tx = createTransaction(date, BigDecimal.valueOf(500), TransactionDirection.NEGATIVE, Currency.HUF);
        when(txRepo.findByBookingDateBetween(any(), any())).thenReturn(List.of(tx));

        KpiDashboardDTO result = service.calculateForPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                BigDecimal.valueOf(2000),
                Currency.HUF);

        assertThat(result.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("500");
        assertThat(result.getStartBalance()).isEqualByComparingTo("2000");
        MonthlyKpiDTO march = result.getMonthlyData().get(2); // Month 3 at index 2
        assertThat(march.getTotalExpense()).isEqualByComparingTo("500");
        assertThat(march.getNetCashFlow()).isEqualByComparingTo("-500");
        assertThat(march.getBankBalance()).isEqualByComparingTo("1500"); // 2000 - 500
    }

    @Test
    void calculateForPlan_uses_historical_transactions() {
        Long planId = 1L;
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setStartBalance(BigDecimal.valueOf(5000));
        plan.setBaseCurrency(Currency.HUF);

        HistoricalTransaction ht = new HistoricalTransaction();
        ht.setTransactionDate(LocalDate.of(2025, 2, 1));
        ht.setAmount(BigDecimal.valueOf(3000));
        TransactionCategory cat = new TransactionCategory();
        cat.setDirection(TransactionDirection.POSITIVE);
        cat.setName("Sales");
        AccountingCategory acct = new AccountingCategory();
        acct.setCode("REV");
        cat.setAccountingCategory(acct);
        ht.setCategory(cat);

        when(planRepo.findById(planId)).thenReturn(Optional.of(plan));
        when(histRepo.findByCashflowPlanId(planId)).thenReturn(List.of(ht));

        KpiDashboardDTO result = service.calculateForPlan(planId);

        assertThat(result.getStartBalance()).isEqualByComparingTo("5000");
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("3000");
        MonthlyKpiDTO feb = result.getMonthlyData().get(1); // Month 2 at index 1
        assertThat(feb.getTotalIncome()).isEqualByComparingTo("3000");
        assertThat(feb.getBankBalance()).isEqualByComparingTo("8000"); // 5000 + 3000
        assertThat(feb.getAccountingCategorySums()).containsEntry("REV", BigDecimal.valueOf(3000));
    }

    @Test
    void calculateForPeriod_accumulates_bank_balance_across_months() {
        Transaction jan = createTransaction(LocalDate.of(2024, 1, 15), BigDecimal.valueOf(1000), TransactionDirection.POSITIVE, Currency.HUF);
        Transaction feb = createTransaction(LocalDate.of(2024, 2, 10), BigDecimal.valueOf(500), TransactionDirection.NEGATIVE, Currency.HUF);
        when(txRepo.findByBookingDateBetween(any(), any())).thenReturn(List.of(jan, feb));

        KpiDashboardDTO result = service.calculateForPeriod(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 12, 31),
                BigDecimal.valueOf(10000),
                Currency.HUF);

        MonthlyKpiDTO janData = result.getMonthlyData().get(0);
        MonthlyKpiDTO febData = result.getMonthlyData().get(1);

        assertThat(janData.getBankBalance()).isEqualByComparingTo("11000"); // 10000 + 1000
        assertThat(febData.getBankBalance()).isEqualByComparingTo("10500"); // 11000 - 500
    }

    private Transaction createTransaction(LocalDate date, BigDecimal amount, TransactionDirection direction, Currency currency) {
        Transaction tx = new Transaction();
        tx.setBookingDate(date);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        TransactionCategory cat = new TransactionCategory();
        cat.setDirection(direction);
        cat.setName("Test Category");
        AccountingCategory acct = new AccountingCategory();
        acct.setCode("TEST");
        cat.setAccountingCategory(acct);
        tx.setCategory(cat);
        return tx;
    }
}
