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
    void getAllAccountingCategories_shouldReturnAllCategories() {
        AccountingCategory cat1 = new AccountingCategory();
        cat1.setId(1L);
        cat1.setCode("REV");
        cat1.setDisplayName("Revenue");
        
        AccountingCategory cat2 = new AccountingCategory();
        cat2.setId(2L);
        cat2.setCode("EXP");
        cat2.setDisplayName("Expenses");
        
        when(acr.findAll()).thenReturn(List.of(cat1, cat2));

        List<AccountingCategory> result = svc.getAllAccountingCategories();

        assertThat(result)
            .hasSize(2)
            .containsExactly(cat1, cat2);
        verify(acr).findAll();
    }

    @Test
    void getAllAccountingCategories_shouldReturnEmptyListWhenNoCategoriesExist() {
        when(acr.findAll()).thenReturn(List.of());

        List<AccountingCategory> result = svc.getAllAccountingCategories();

        assertThat(result).isEmpty();
        verify(acr).findAll();
    }

    @Test
    void updateTransactionCategoryMapping_shouldThrowExceptionWhenAccountingCategoryNotFound() {
        Long accountingCategoryId = 999L;
        when(acr.findById(accountingCategoryId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> svc.updateTransactionCategoryMapping(accountingCategoryId, List.of(1L)));
        
        verify(acr).findById(accountingCategoryId);
        verify(tcr, never()).findById(any());
        verify(tcr, never()).save(any());
    }

    @Test
    void updateTransactionCategoryMapping_shouldMapValidTransactionCategories() {
        AccountingCategory accountingCategory = new AccountingCategory();
        accountingCategory.setId(10L);
        accountingCategory.setCode("OPR");
        accountingCategory.setDisplayName("Operating");
        when(acr.findById(10L)).thenReturn(Optional.of(accountingCategory));

        TransactionCategory tc1 = new TransactionCategory();
        tc1.setId(1L);
        
        TransactionCategory tc2 = new TransactionCategory();
        tc2.setId(2L);

        when(tcr.findById(1L)).thenReturn(Optional.of(tc1));
        when(tcr.findById(2L)).thenReturn(Optional.of(tc2));

        svc.updateTransactionCategoryMapping(10L, List.of(1L, 2L));

        assertThat(tc1.getAccountingCategory()).isSameAs(accountingCategory);
        assertThat(tc2.getAccountingCategory()).isSameAs(accountingCategory);
        verify(tcr).save(tc1);
        verify(tcr).save(tc2);
    }

    @Test
    void updateTransactionCategoryMapping_shouldSkipNonexistentTransactionCategories() {
        AccountingCategory accountingCategory = new AccountingCategory();
        accountingCategory.setId(10L);
        when(acr.findById(10L)).thenReturn(Optional.of(accountingCategory));

        TransactionCategory tc1 = new TransactionCategory();
        tc1.setId(1L);

        when(tcr.findById(1L)).thenReturn(Optional.of(tc1));
        when(tcr.findById(2L)).thenReturn(Optional.empty()); // Not found
        when(tcr.findById(3L)).thenReturn(Optional.empty()); // Not found

        svc.updateTransactionCategoryMapping(10L, List.of(1L, 2L, 3L));

        assertThat(tc1.getAccountingCategory()).isSameAs(accountingCategory);
        verify(tcr).save(tc1);
        verify(tcr, times(1)).save(any()); // Only one save
    }

    @Test
    void updateTransactionCategoryMapping_shouldHandleEmptyTransactionCategoryList() {
        AccountingCategory accountingCategory = new AccountingCategory();
        accountingCategory.setId(10L);
        when(acr.findById(10L)).thenReturn(Optional.of(accountingCategory));

        svc.updateTransactionCategoryMapping(10L, List.of());

        verify(acr).findById(10L);
        verify(tcr, never()).findById(any());
        verify(tcr, never()).save(any());
    }

    @Test
    void updateTransactionCategoryMapping_shouldHandleSingleMapping() {
        AccountingCategory accountingCategory = new AccountingCategory();
        accountingCategory.setId(5L);
        when(acr.findById(5L)).thenReturn(Optional.of(accountingCategory));

        TransactionCategory tc = new TransactionCategory();
        tc.setId(100L);
        when(tcr.findById(100L)).thenReturn(Optional.of(tc));

        svc.updateTransactionCategoryMapping(5L, List.of(100L));

        assertThat(tc.getAccountingCategory()).isSameAs(accountingCategory);
        verify(tcr).save(tc);
    }

    @Test
    void updateTransactionCategoryMapping_shouldHandleAllInvalidTransactionCategories() {
        AccountingCategory accountingCategory = new AccountingCategory();
        accountingCategory.setId(10L);
        when(acr.findById(10L)).thenReturn(Optional.of(accountingCategory));

        when(tcr.findById(1L)).thenReturn(Optional.empty());
        when(tcr.findById(2L)).thenReturn(Optional.empty());

        svc.updateTransactionCategoryMapping(10L, List.of(1L, 2L));

        verify(acr).findById(10L);
        verify(tcr).findById(1L);
        verify(tcr).findById(2L);
        verify(tcr, never()).save(any()); // No saves
    }
}
