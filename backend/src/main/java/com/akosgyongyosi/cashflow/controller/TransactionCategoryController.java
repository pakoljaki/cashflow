package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class TransactionCategoryController {

    private final TransactionCategoryRepository categoryRepository;

    public TransactionCategoryController(TransactionCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<TransactionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@RequestBody CategoryRequest request) {
        // Optionally check if name already exists, etc.
        if (request.getName() == null || request.getDirection() == null) {
            return ResponseEntity.badRequest().body("Category name/direction missing");
        }

        TransactionCategory newCat = new TransactionCategory();
        newCat.setName(request.getName());
        newCat.setDirection(request.getDirection());
        newCat.setDescription("Auto-created category: " + request.getName());

        TransactionCategory saved = categoryRepository.save(newCat);

        return ResponseEntity.ok(saved); // returns the newly created category
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
