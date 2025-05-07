// src/main/java/com/akosgyongyosi/cashflow/controller/TransactionCategoryController.java
package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class TransactionCategoryController {

    private final TransactionCategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public TransactionCategoryController(
            TransactionCategoryRepository categoryRepository,
            TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<TransactionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        if (request.getName() == null || request.getDirection() == null) {
            return ResponseEntity.badRequest().body("Category name/direction missing");
        }

        TransactionCategory newCat = new TransactionCategory();
        newCat.setName(request.getName());
        newCat.setDirection(request.getDirection());
        newCat.setDescription("Auto-created category: " + request.getName());

        TransactionCategory saved = categoryRepository.save(newCat);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        long inUse = transactionRepository.countByCategoryId(id);
        if (inUse > 0) {
            return ResponseEntity
                    .badRequest()
                    .body("Cannot delete: category is still assigned to " + inUse + " transaction(s).");
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    public static class CategoryRequest {
        private String name;
        private com.akosgyongyosi.cashflow.entity.TransactionDirection direction;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public com.akosgyongyosi.cashflow.entity.TransactionDirection getDirection() { return direction; }
        public void setDirection(com.akosgyongyosi.cashflow.entity.TransactionDirection direction) {
            this.direction = direction;
        }
    }
}
