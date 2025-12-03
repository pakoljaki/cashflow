package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.CategoryRequestDTO;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/categories")
public class TransactionCategoryController {

    private static final Logger log = LoggerFactory.getLogger(TransactionCategoryController.class);

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
    public ResponseEntity<Object> createCategory(@RequestBody CategoryRequestDTO request) {
        String name = Objects.requireNonNullElse(request.getName(), "").trim();
        if (name.isEmpty() || request.getDirection() == null) {
            return ResponseEntity.badRequest().body("Category name/direction missing");
        }

        if (categoryRepository.findByName(name).isPresent()) {
            return ResponseEntity.badRequest().body("Category already exists");
        }

        TransactionCategory newCat = new TransactionCategory();
        newCat.setName(name);
        newCat.setDirection(request.getDirection());
        newCat.setDescription("Auto-created category: " + name);

        TransactionCategory saved = categoryRepository.save(newCat);
        log.debug("Created category {} with direction {}", name, request.getDirection());
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteCategory(@PathVariable long id) {
        long inUse = transactionRepository.countByCategoryId(id);
        if (inUse > 0) {
            return ResponseEntity
                    .badRequest()
                    .body("Cannot delete: category is still assigned to " + inUse + " transaction(s).");
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
