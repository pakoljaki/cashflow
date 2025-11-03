package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.UpdateCategoryMappingRequestDTO;
import com.akosgyongyosi.cashflow.entity.AccountingCategory;
import com.akosgyongyosi.cashflow.repository.AccountingCategoryRepository;
import com.akosgyongyosi.cashflow.service.AccountingCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountingCategoryControllerTest {

    private AccountingCategoryService accountingCategoryService;
    private AccountingCategoryRepository accountingCategoryRepository;
    private AccountingCategoryController controller;

    @BeforeEach
    void setUp() {
        accountingCategoryService = mock(AccountingCategoryService.class);
        accountingCategoryRepository = mock(AccountingCategoryRepository.class);
        controller = new AccountingCategoryController(accountingCategoryService, accountingCategoryRepository);
    }

    @Test
    void getAllAccountingCategories_returns_all_categories() {
        AccountingCategory cat1 = new AccountingCategory();
        cat1.setCode("REV");
        AccountingCategory cat2 = new AccountingCategory();
        cat2.setCode("EXP");
        when(accountingCategoryService.getAllAccountingCategories()).thenReturn(List.of(cat1, cat2));

        List<AccountingCategory> result = controller.getAllAccountingCategories();

        assertThat(result).hasSize(2);
        verify(accountingCategoryService).getAllAccountingCategories();
    }

    @Test
    void createAccountingCategory_saves_and_returns_category() {
        AccountingCategory newCat = new AccountingCategory();
        newCat.setCode("NEW");
        newCat.setDisplayName("New Category");

        AccountingCategory savedCat = new AccountingCategory();
        savedCat.setId(1L);
        savedCat.setCode("NEW");
        savedCat.setDisplayName("New Category");
        when(accountingCategoryRepository.save(newCat)).thenReturn(savedCat);

        var response = controller.createAccountingCategory(newCat);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(savedCat);
        verify(accountingCategoryRepository).save(newCat);
    }

    @Test
    void updateMapping_updates_transaction_category_mapping() {
        UpdateCategoryMappingRequestDTO request = new UpdateCategoryMappingRequestDTO();
        request.setAccountingCategoryId(1L);
        request.setTransactionCategoryIds(List.of(10L, 20L, 30L));

        doNothing().when(accountingCategoryService).updateTransactionCategoryMapping(1L, List.of(10L, 20L, 30L));

        var response = controller.updateMapping(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(accountingCategoryService).updateTransactionCategoryMapping(1L, List.of(10L, 20L, 30L));
    }

    @Test
    void updateMapping_returns_bad_request_when_accounting_category_id_missing() {
        UpdateCategoryMappingRequestDTO request = new UpdateCategoryMappingRequestDTO();
        request.setAccountingCategoryId(null);
        request.setTransactionCategoryIds(List.of(10L));

        var response = controller.updateMapping(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(accountingCategoryService, never()).updateTransactionCategoryMapping(any(), any());
    }

    @Test
    void updateMapping_returns_bad_request_when_transaction_category_ids_missing() {
        UpdateCategoryMappingRequestDTO request = new UpdateCategoryMappingRequestDTO();
        request.setAccountingCategoryId(1L);
        request.setTransactionCategoryIds(null);

        var response = controller.updateMapping(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(accountingCategoryService, never()).updateTransactionCategoryMapping(any(), any());
    }
}
