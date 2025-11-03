package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import com.akosgyongyosi.cashflow.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CashflowPlanServiceTest {

    private CashflowPlanRepository planRepo;
    private TransactionRepository txRepo;
    private FxService fxService;
    private CashflowPlanService svc;

    @BeforeEach
    void setUp() {
        planRepo = mock(CashflowPlanRepository.class);
        txRepo = mock(TransactionRepository.class);
        fxService = mock(FxService.class);
        svc = new CashflowPlanService(planRepo, txRepo, fxService);
    }

    @Test
    void getPlan_delegatesToRepo() {
        CashflowPlan p = new CashflowPlan();
        when(planRepo.findById(1L)).thenReturn(Optional.of(p));
        assertThat(svc.getPlan(1L)).contains(p);
    }

    @Test
    void findAll_delegatesToRepo() {
        when(planRepo.findAll()).thenReturn(List.of(new CashflowPlan()));
        assertThat(svc.findAll()).hasSize(1);
    }

    @Test
    void createPlanForInterval_buildsAndSavesHistorical() {
        LocalDate start = LocalDate.of(2025,1,1);
        LocalDate end = LocalDate.of(2025,12,31);
        Transaction tx = new Transaction();
        tx.setBookingDate(start.minusYears(1).plusDays(1)); // 2024-01-02
        tx.setAmount(BigDecimal.valueOf(123));
        tx.setCurrency(Currency.HUF);
        TransactionCategory cat = new TransactionCategory();
        AccountingCategory acct = new AccountingCategory();
        acct.setCode("REV");
        cat.setAccountingCategory(acct);
        cat.setDirection(TransactionDirection.POSITIVE);
        tx.setCategory(cat);
        when(txRepo.findByBookingDateBetween(start.minusYears(1), end.minusYears(1)))
            .thenReturn(List.of(tx));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CashflowPlan result = svc.createPlanForInterval("Base", start, end, ScenarioType.BEST, BigDecimal.TEN, UUID.randomUUID().toString());
        assertThat(result.getBaselineTransactions()).hasSize(1);
        HistoricalTransaction hist = result.getBaselineTransactions().get(0);
        assertThat(hist.getTransactionDate()).isEqualTo(tx.getBookingDate().plusYears(1));
        assertThat(hist.getOriginalAmount()).isEqualByComparingTo("123");
    }

    @Test
    void createAllScenarioPlans_returnsThreeWithDistinctKeys() {
        LocalDate s = LocalDate.now();
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        List<CashflowPlan> list = svc.createAllScenarioPlans("X", s, s, BigDecimal.ZERO);
        assertThat(list).hasSize(3)
            .extracting(CashflowPlan::getScenario)
            .containsExactly(ScenarioType.WORST, ScenarioType.REALISTIC, ScenarioType.BEST);
        assertThat(list)
            .extracting(CashflowPlan::getGroupKey)
            .allMatch(key -> key.equals(list.get(0).getGroupKey()));
    }

    @Test
    void findAllByGroupKey_delegates() {
        when(planRepo.findByGroupKey("k")).thenReturn(List.of(new CashflowPlan()));
        assertThat(svc.findAllByGroupKey("k")).hasSize(1);
    }

    @Test
    void snapshot_converts_foreign_currency_to_base() {
        LocalDate start = LocalDate.of(2025,1,1);
        LocalDate end = LocalDate.of(2025,12,31);
        // EUR transaction (should be converted)
        Transaction eurTx = new Transaction();
        eurTx.setBookingDate(LocalDate.of(2024,6,15));
        eurTx.setAmount(BigDecimal.valueOf(2500));
        eurTx.setCurrency(Currency.EUR);
        TransactionCategory catEur = new TransactionCategory();
        catEur.setDirection(TransactionDirection.POSITIVE);
        eurTx.setCategory(catEur);
        // HUF transaction ensures base currency inferred as HUF
        Transaction hufTx = new Transaction();
        hufTx.setBookingDate(LocalDate.of(2024,3,10));
        hufTx.setAmount(BigDecimal.valueOf(10000));
        hufTx.setCurrency(Currency.HUF);
        TransactionCategory catHuf = new TransactionCategory();
        catHuf.setDirection(TransactionDirection.POSITIVE);
        hufTx.setCategory(catHuf);
        when(txRepo.findByBookingDateBetween(start.minusYears(1), end.minusYears(1)))
            .thenReturn(List.of(eurTx, hufTx));
        // Mock FX conversion: EUR->HUF rate for 2024-06-15 is 400 (convert 1 EUR)
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, eurTx.getBookingDate()))
            .thenReturn(BigDecimal.valueOf(400));
        when(planRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CashflowPlan plan = svc.createPlanForInterval("FX", start, end, ScenarioType.REALISTIC, BigDecimal.ZERO, UUID.randomUUID().toString());
        assertThat(plan.getBaseCurrency()).isEqualTo(Currency.HUF); // inferred base due to presence of HUF transaction
        assertThat(plan.getBaselineTransactions()).hasSize(2);
        HistoricalTransaction eurHist = plan.getBaselineTransactions().stream()
            .filter(ht -> ht.getOriginalCurrency() == Currency.EUR)
            .findFirst().orElseThrow();
        // 2500 * 400 = 1,000,000
        assertThat(eurHist.getAmount()).isEqualByComparingTo("1000000");
        HistoricalTransaction hufHist = plan.getBaselineTransactions().stream()
            .filter(ht -> ht.getOriginalCurrency() == Currency.HUF)
            .findFirst().orElseThrow();
        assertThat(hufHist.getAmount()).isEqualByComparingTo("10000"); // unchanged
    }
}
