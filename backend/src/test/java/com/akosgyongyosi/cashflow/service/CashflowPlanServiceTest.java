package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    private CashflowPlanService svc;

    @BeforeEach
    void setUp() {
        planRepo = mock(CashflowPlanRepository.class);
        txRepo = mock(TransactionRepository.class);
        svc = new CashflowPlanService(planRepo, txRepo);
    }

    /*@Test
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
        LocalDate start = LocalDate.of(2024,1,1);
        LocalDate end = LocalDate.of(2024,12,31);
        Transaction tx = new Transaction();
        tx.setBookingDate(start.minusYears(1).plusDays(1));
        tx.setAmount(BigDecimal.valueOf(123));
        tx.setCategory(new TransactionCategory());
        when(txRepo.findByBookingDateBetween(start.minusYears(1), end.minusYears(1)))
            .thenReturn(List.of(tx));
        CashflowPlan saved = new CashflowPlan();
        when(planRepo.save(any())).thenReturn(saved);

        CashflowPlan result = svc.createPlanForInterval("N", start, end, ScenarioType.BEST, BigDecimal.TEN, "gk");
        assertThat(result).isSameAs(saved);

        ArgumentCaptor<CashflowPlan> cap = ArgumentCaptor.forClass(CashflowPlan.class);
        verify(planRepo).save(cap.capture());
        CashflowPlan built = cap.getValue();
        assertThat(built.getBaselineTransactions())
            .singleElement()
            .extracting(HistoricalTransaction::getTransactionDate)
            .isEqualTo(tx.getBookingDate().plusYears(1));
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
    }*/
}
