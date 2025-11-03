package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionCategoryControllerTest {

    @Mock
    private TransactionCategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionCategoryController transactionCategoryController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllCategories_shouldReturnAllCategories() {
        TransactionCategory cat1 = new TransactionCategory();
        cat1.setId(1L);
        cat1.setName("Groceries");
        cat1.setDirection(TransactionDirection.NEGATIVE);

        TransactionCategory cat2 = new TransactionCategory();
        cat2.setId(2L);
        cat2.setName("Salary");
        cat2.setDirection(TransactionDirection.POSITIVE);

        when(categoryRepository.findAll()).thenReturn(Arrays.asList(cat1, cat2));

        List<TransactionCategory> result = transactionCategoryController.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Groceries");
        assertThat(result.get(1).getName()).isEqualTo("Salary");
        verify(categoryRepository).findAll();
    }

    @Test
    void createCategory_shouldCreateNewCategory() {
        TransactionCategoryController.CategoryRequest request = new TransactionCategoryController.CategoryRequest();
        request.setName("New Category");
        request.setDirection(TransactionDirection.NEGATIVE);

        TransactionCategory savedCategory = new TransactionCategory();
        savedCategory.setId(1L);
        savedCategory.setName("New Category");
        savedCategory.setDirection(TransactionDirection.NEGATIVE);

        when(categoryRepository.save(any(TransactionCategory.class))).thenReturn(savedCategory);

        ResponseEntity<?> response = transactionCategoryController.createCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TransactionCategory result = (TransactionCategory) response.getBody();
        assertThat(result.getName()).isEqualTo("New Category");
        assertThat(result.getDirection()).isEqualTo(TransactionDirection.NEGATIVE);
        verify(categoryRepository).save(any(TransactionCategory.class));
    }

    @Test
    void createCategory_shouldReturnBadRequestWhenNameIsMissing() {
        TransactionCategoryController.CategoryRequest request = new TransactionCategoryController.CategoryRequest();
        request.setDirection(TransactionDirection.NEGATIVE);

        ResponseEntity<?> response = transactionCategoryController.createCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Category name/direction missing");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategory_shouldReturnBadRequestWhenDirectionIsMissing() {
        TransactionCategoryController.CategoryRequest request = new TransactionCategoryController.CategoryRequest();
        request.setName("New Category");

        ResponseEntity<?> response = transactionCategoryController.createCategory(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Category name/direction missing");
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void deleteCategory_shouldDeleteCategoryWhenNotInUse() {
        Long categoryId = 1L;
        when(transactionRepository.countByCategoryId(categoryId)).thenReturn(0L);

        ResponseEntity<?> response = transactionCategoryController.deleteCategory(categoryId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(transactionRepository).countByCategoryId(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    void deleteCategory_shouldReturnBadRequestWhenCategoryInUse() {
        Long categoryId = 1L;
        when(transactionRepository.countByCategoryId(categoryId)).thenReturn(5L);

        ResponseEntity<?> response = transactionCategoryController.deleteCategory(categoryId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().toString()).contains("Cannot delete");
        assertThat(response.getBody().toString()).contains("5 transaction(s)");
        verify(transactionRepository).countByCategoryId(categoryId);
        verify(categoryRepository, never()).deleteById(any());
    }
}
