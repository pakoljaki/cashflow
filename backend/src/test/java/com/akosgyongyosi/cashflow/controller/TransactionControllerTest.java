package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionControllerTest {

    private TransactionRepository transactionRepository;
    private TransactionCategoryRepository categoryRepository;
    private TransactionController controller;

    @BeforeEach
    void setUp() {
        transactionRepository = mock(TransactionRepository.class);
        categoryRepository = mock(TransactionCategoryRepository.class);
        controller = new TransactionController(transactionRepository, categoryRepository);
    }

    @Test
    void getAllTransactions_returns_all_transactions() {
        Transaction tx1 = createTransaction(1L, "TX1");
        Transaction tx2 = createTransaction(2L, "TX2");
        when(transactionRepository.findAll()).thenReturn(List.of(tx1, tx2));

        List<Transaction> result = controller.getAllTransactions();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(tx1, tx2);
    }

    @Test
    void getAllCategories_returns_all_categories() {
        TransactionCategory cat1 = new TransactionCategory();
        cat1.setName("Category 1");
        TransactionCategory cat2 = new TransactionCategory();
        cat2.setName("Category 2");
        when(categoryRepository.findAll()).thenReturn(List.of(cat1, cat2));

        List<TransactionCategory> result = controller.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(cat1, cat2);
    }

    @Test
    void getCategoriesByDirection_filters_by_direction() {
        TransactionCategory cat1 = new TransactionCategory();
        cat1.setDirection(TransactionDirection.POSITIVE);
        when(categoryRepository.findByDirection(TransactionDirection.POSITIVE))
                .thenReturn(List.of(cat1));

        var response = controller.getCategoriesByDirection(TransactionDirection.POSITIVE);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getDirection()).isEqualTo(TransactionDirection.POSITIVE);
    }

    @Test
    void updateTransactionCategory_with_new_category_creates_and_assigns() {
        Transaction tx = createTransaction(1L, "TX1");
        tx.setTransactionDirection(TransactionDirection.POSITIVE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        TransactionCategory savedCat = new TransactionCategory();
        savedCat.setId(10L);
        savedCat.setName("New Category");
        when(categoryRepository.save(any(TransactionCategory.class))).thenReturn(savedCat);

        TransactionController.CategoryUpdateRequest request = new TransactionController.CategoryUpdateRequest();
        request.setCategoryName("New Category");
        request.setCreateNewCategory(true);
        request.setDescription("Test description");

        var response = controller.updateTransactionCategory(1L, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(categoryRepository).save(argThat(cat ->
                cat.getName().equals("New Category") &&
                cat.getDirection() == TransactionDirection.POSITIVE
        ));
        verify(transactionRepository).save(tx);
    }

    @Test
    void updateTransactionCategory_with_existing_category_assigns_it() {
        Transaction tx = createTransaction(1L, "TX1");
        tx.setTransactionDirection(TransactionDirection.POSITIVE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        TransactionCategory existingCat = new TransactionCategory();
        existingCat.setId(5L);
        existingCat.setName("Existing Category");
        existingCat.setDirection(TransactionDirection.POSITIVE);
        when(categoryRepository.findByName("Existing Category")).thenReturn(Optional.of(existingCat));

        TransactionController.CategoryUpdateRequest request = new TransactionController.CategoryUpdateRequest();
        request.setCategoryName("Existing Category");
        request.setCreateNewCategory(false);

        var response = controller.updateTransactionCategory(1L, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(tx.getCategory()).isEqualTo(existingCat);
        verify(transactionRepository).save(tx);
    }

    @Test
    void updateTransactionCategory_returns_error_when_transaction_not_found() {
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        TransactionController.CategoryUpdateRequest request = new TransactionController.CategoryUpdateRequest();
        request.setCategoryName("Category");

        var response = controller.updateTransactionCategory(999L, request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateTransactionCategory_returns_error_when_category_not_found() {
        Transaction tx = createTransaction(1L, "TX1");
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));
        when(categoryRepository.findByName("Nonexistent")).thenReturn(Optional.empty());

        TransactionController.CategoryUpdateRequest request = new TransactionController.CategoryUpdateRequest();
        request.setCategoryName("Nonexistent");
        request.setCreateNewCategory(false);

        var response = controller.updateTransactionCategory(1L, request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void updateTransactionCategory_returns_error_when_direction_mismatch() {
        Transaction tx = createTransaction(1L, "TX1");
        tx.setTransactionDirection(TransactionDirection.POSITIVE);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(tx));

        TransactionCategory cat = new TransactionCategory();
        cat.setDirection(TransactionDirection.NEGATIVE);
        when(categoryRepository.findByName("Category")).thenReturn(Optional.of(cat));

        TransactionController.CategoryUpdateRequest request = new TransactionController.CategoryUpdateRequest();
        request.setCategoryName("Category");
        request.setCreateNewCategory(false);

        var response = controller.updateTransactionCategory(1L, request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody().toString()).contains("direction mismatch");
    }

    @Test
    void assignBulkCategory_assigns_category_to_multiple_transactions() {
        Transaction tx1 = createTransaction(1L, "TX1");
        tx1.setTransactionDirection(TransactionDirection.POSITIVE);
        Transaction tx2 = createTransaction(2L, "TX2");
        tx2.setTransactionDirection(TransactionDirection.POSITIVE);

        TransactionCategory cat = new TransactionCategory();
        cat.setId(10L);
        cat.setDirection(TransactionDirection.POSITIVE);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat));
        when(transactionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(tx1, tx2));

        TransactionController.BulkCategoryRequest request = new TransactionController.BulkCategoryRequest();
        request.setTransactionIds(List.of(1L, 2L));
        request.setCategoryId(10L);

        var response = controller.assignBulkCategory(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(tx1.getCategory()).isEqualTo(cat);
        assertThat(tx2.getCategory()).isEqualTo(cat);
        verify(transactionRepository).saveAll(List.of(tx1, tx2));
    }

    @Test
    void assignBulkCategory_returns_error_when_no_transaction_ids() {
        TransactionController.BulkCategoryRequest request = new TransactionController.BulkCategoryRequest();
        request.setTransactionIds(List.of());
        request.setCategoryId(10L);

        var response = controller.assignBulkCategory(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void assignBulkCategory_returns_error_when_category_not_found() {
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        TransactionController.BulkCategoryRequest request = new TransactionController.BulkCategoryRequest();
        request.setTransactionIds(List.of(1L, 2L));
        request.setCategoryId(999L);

        var response = controller.assignBulkCategory(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void assignBulkCategory_returns_error_when_mixed_directions() {
        Transaction tx1 = createTransaction(1L, "TX1");
        tx1.setTransactionDirection(TransactionDirection.POSITIVE);
        Transaction tx2 = createTransaction(2L, "TX2");
        tx2.setTransactionDirection(TransactionDirection.NEGATIVE);

        TransactionCategory cat = new TransactionCategory();
        cat.setId(10L);

        when(categoryRepository.findById(10L)).thenReturn(Optional.of(cat));
        when(transactionRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(tx1, tx2));

        TransactionController.BulkCategoryRequest request = new TransactionController.BulkCategoryRequest();
        request.setTransactionIds(List.of(1L, 2L));
        request.setCategoryId(10L);

        var response = controller.assignBulkCategory(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody().toString()).contains("Cannot mix");
    }

    @Test
    void createCategory_creates_new_category() {
        TransactionCategory newCat = new TransactionCategory();
        newCat.setName("New Category");
        when(categoryRepository.findByName("New Category")).thenReturn(Optional.empty());
        when(categoryRepository.save(newCat)).thenReturn(newCat);

        var response = controller.createCategory(newCat);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(categoryRepository).save(newCat);
    }

    @Test
    void createCategory_returns_error_when_category_exists() {
        TransactionCategory existing = new TransactionCategory();
        existing.setName("Existing");
        when(categoryRepository.findByName("Existing")).thenReturn(Optional.of(existing));

        TransactionCategory newCat = new TransactionCategory();
        newCat.setName("Existing");

        var response = controller.createCategory(newCat);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(categoryRepository, never()).save(any());
    }

    private Transaction createTransaction(Long id, String memo) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setMemo(memo);
        tx.setAmount(BigDecimal.valueOf(100));
        tx.setCurrency(Currency.HUF);
        tx.setBookingDate(LocalDate.now());
        tx.setTransactionDirection(TransactionDirection.POSITIVE);
        return tx;
    }
}
