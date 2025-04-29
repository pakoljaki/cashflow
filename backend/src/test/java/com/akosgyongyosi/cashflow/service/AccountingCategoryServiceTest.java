package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.AccountingCategory;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.repository.AccountingCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AccountingCategoryServiceTest {

    private AccountingCategoryRepository acr;
    private TransactionCategoryRepository tcr;
    private AccountingCategoryService svc;

    @BeforeEach
    void setUp() {
        acr = mock(AccountingCategoryRepository.class);
        tcr = mock(TransactionCategoryRepository.class);
        svc = new AccountingCategoryService(acr, tcr);
    }

    @Test
    void getAllAccountingCategories_delegates() {
        when(acr.findAll()).thenReturn(List.of(new AccountingCategory()));
        assertThat(svc.getAllAccountingCategories()).hasSize(1);
    }

    @Test
    void updateTransactionCategoryMapping_throwsWhenMissing() {
        when(acr.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
            () -> svc.updateTransactionCategoryMapping(2L, List.of(1L)));
    }

    @Test
    void updateTransactionCategoryMapping_savesValidMappings() {
        AccountingCategory a = new AccountingCategory();
        when(acr.findById(10L)).thenReturn(Optional.of(a));

        TransactionCategory t1 = new TransactionCategory();
        t1.setId(1L);
        TransactionCategory t2 = new TransactionCategory();
        t2.setId(2L);

        when(tcr.findById(1L)).thenReturn(Optional.of(t1));
        when(tcr.findById(2L)).thenReturn(Optional.empty());

        svc.updateTransactionCategoryMapping(10L, List.of(1L, 2L));

        assertThat(t1.getAccountingCategory()).isSameAs(a);
        verify(tcr).save(t1);
    }
}
