package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.BulkCategoryRequestDTO;
import com.akosgyongyosi.cashflow.dto.CategoryUpdateRequestDTO;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;

    public TransactionController(TransactionRepository transactionRepository,
                                 TransactionCategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }
    
    @GetMapping
    public List<Transaction> getAllTransactions() {
        List<Transaction> allTx = transactionRepository.findAll();
        log.debug("getAllTransactions() returning {} rows", allTx.size());
        return allTx;
    }
    
    @GetMapping("/categories")
    public List<TransactionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @GetMapping("/categories/direction")
    public ResponseEntity<List<TransactionCategory>> getCategoriesByDirection(
            @RequestParam TransactionDirection direction) {
        List<TransactionCategory> list = categoryRepository.findByDirection(direction);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{transactionId}/category")
    public ResponseEntity<Object> updateTransactionCategory(
            @PathVariable long transactionId,
            @RequestBody CategoryUpdateRequestDTO request) {

        Optional<Transaction> txOpt = transactionRepository.findById(transactionId);
        if (txOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Transaction not found with id: " + transactionId);
        }
        Transaction transaction = txOpt.get();
        TransactionDirection direction = transaction.getTransactionDirection();

        if (request.isCreateNewCategory()) {
            TransactionCategory newCat = new TransactionCategory();
            newCat.setName(request.getCategoryName());
            newCat.setDirection(direction);
            newCat.setDescription(request.getDescription());

            TransactionCategory savedCategory = categoryRepository.save(newCat);
            transaction.setCategory(savedCategory);
        } else {
            Optional<TransactionCategory> categoryOpt = categoryRepository.findByName(request.getCategoryName());
            if (categoryOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Category not found with name: " + request.getCategoryName());
            }

            TransactionCategory existingCat = categoryOpt.get();
            if (!existingCat.getDirection().equals(direction)) {
                return ResponseEntity.badRequest().body("Category direction mismatch! " +
                        "Transaction is " + direction + " but category is " + existingCat.getDirection());
            }

            transaction.setCategory(existingCat);
        }

        transactionRepository.save(transaction);
        return ResponseEntity.ok("Transaction category updated");
    }

    @PutMapping("/bulk-category")
    public ResponseEntity<Object> assignBulkCategory(@RequestBody BulkCategoryRequestDTO request) {
        if (request.getTransactionIds() == null || request.getTransactionIds().isEmpty()) {
            return ResponseEntity.badRequest().body("No transaction IDs provided.");
        }

        if (request.getCategoryId() == null) {
            return ResponseEntity.badRequest().body("No categoryId provided.");
        }

        Long categoryId = Objects.requireNonNull(request.getCategoryId(), "categoryId must not be null");
        Optional<TransactionCategory> catOpt = categoryRepository.findById(categoryId);
        if (catOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Category not found with ID: " + request.getCategoryId());
        }
        TransactionCategory category = catOpt.get();

        List<Long> transactionIds = Objects.requireNonNull(request.getTransactionIds());
        List<Transaction> txList = transactionRepository.findAllById(transactionIds);
        if (txList.isEmpty()) {
            return ResponseEntity.badRequest().body("No matching transactions found for given IDs.");
        }

        TransactionDirection direction = txList.get(0).getTransactionDirection();
        for (Transaction tx : txList) {
            if (!tx.getTransactionDirection().equals(direction)) {
                return ResponseEntity.badRequest().body("Cannot mix POSITIVE and NEGATIVE transactions for categorization.");
            }
        }

        txList.forEach(tx -> tx.setCategory(category));
        transactionRepository.saveAll(txList);

        return ResponseEntity.ok("Category assigned to " + txList.size() + " transactions.");
    }

    @PostMapping("/categories")
    public ResponseEntity<Object> createCategory(@RequestBody TransactionCategory newCategory) {
        if (categoryRepository.findByName(newCategory.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Category already exists.");
        }

        TransactionCategory savedCategory = categoryRepository.save(newCategory);
        return ResponseEntity.ok(savedCategory);
    }
}
