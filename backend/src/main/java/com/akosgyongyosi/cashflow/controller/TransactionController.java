package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for managing transactions and categories.
 */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

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
        System.out.println("DEBUG: getAllTransactions() returning " + allTx.size() + " rows");
        return allTx;
    }
    



    /**
     * ✅ Fetch all transaction categories.
     */
    @GetMapping("/categories")
    public List<TransactionCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * ✅ Fetch categories by transaction direction (POSITIVE or NEGATIVE).
     */
    @GetMapping("/categories/direction")
    public ResponseEntity<List<TransactionCategory>> getCategoriesByDirection(
            @RequestParam TransactionDirection direction) {
        List<TransactionCategory> list = categoryRepository.findByDirection(direction);
        return ResponseEntity.ok(list);
    }

    /**
     * ✅ Update the category of a single transaction.
     */
    @PutMapping("/{transactionId}/category")
    public ResponseEntity<?> updateTransactionCategory(
            @PathVariable Long transactionId,
            @RequestBody CategoryUpdateRequest request) {

        Optional<Transaction> txOpt = transactionRepository.findById(transactionId);
        if (txOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Transaction not found with id: " + transactionId);
        }
        Transaction transaction = txOpt.get();
        TransactionDirection direction = transaction.getTransactionDirection();

        // ✅ Create a new category if requested
        if (request.isCreateNewCategory()) {
            TransactionCategory newCat = new TransactionCategory();
            newCat.setName(request.getCategoryName());
            newCat.setDirection(direction);
            newCat.setDescription(request.getDescription());

            TransactionCategory savedCategory = categoryRepository.save(newCat);
            transaction.setCategory(savedCategory);
        } else {
            // ✅ Assign existing category
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

    /**
     * ✅ Bulk update: Assign multiple transactions to a single category.
     */
    @PutMapping("/bulk-category")
    public ResponseEntity<?> assignBulkCategory(@RequestBody BulkCategoryRequest request) {
        if (request.getTransactionIds() == null || request.getTransactionIds().isEmpty()) {
            return ResponseEntity.badRequest().body("No transaction IDs provided.");
        }

        if (request.getCategoryId() == null) {
            return ResponseEntity.badRequest().body("No categoryId provided.");
        }

        // Find category
        Optional<TransactionCategory> catOpt = categoryRepository.findById(request.getCategoryId());
        if (catOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Category not found with ID: " + request.getCategoryId());
        }
        TransactionCategory category = catOpt.get();

        // Find transactions
        List<Transaction> txList = transactionRepository.findAllById(request.getTransactionIds());
        if (txList.isEmpty()) {
            return ResponseEntity.badRequest().body("No matching transactions found for given IDs.");
        }

        // Ensure all transactions have the same direction
        TransactionDirection direction = txList.get(0).getTransactionDirection();
        for (Transaction tx : txList) {
            if (!tx.getTransactionDirection().equals(direction)) {
                return ResponseEntity.badRequest().body("Cannot mix POSITIVE and NEGATIVE transactions for categorization.");
            }
        }

        // Assign the category
        txList.forEach(tx -> tx.setCategory(category));
        transactionRepository.saveAll(txList);

        return ResponseEntity.ok("Category assigned to " + txList.size() + " transactions.");
    }

    /**
     * ✅ Create a new category.
     */
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody TransactionCategory newCategory) {
        if (categoryRepository.findByName(newCategory.getName()).isPresent()) {
            return ResponseEntity.badRequest().body("Category already exists.");
        }

        TransactionCategory savedCategory = categoryRepository.save(newCategory);
        return ResponseEntity.ok(savedCategory);
    }

    /**
     * DTO for updating a single transaction's category.
     */
    public static class CategoryUpdateRequest {
        private String categoryName;
        private boolean createNewCategory;
        private String description;

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public boolean isCreateNewCategory() { return createNewCategory; }
        public void setCreateNewCategory(boolean createNewCategory) { this.createNewCategory = createNewCategory; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * DTO for bulk updating transaction categories.
     */
    public static class BulkCategoryRequest {
        private List<Long> transactionIds;
        private Long categoryId;

        public List<Long> getTransactionIds() { return transactionIds; }
        public void setTransactionIds(List<Long> transactionIds) { this.transactionIds = transactionIds; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    }
}
