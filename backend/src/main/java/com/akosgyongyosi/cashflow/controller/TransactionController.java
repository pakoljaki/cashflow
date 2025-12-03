package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.BulkCategoryRequestDTO;
import com.akosgyongyosi.cashflow.dto.CategoryUpdateRequestDTO;
import com.akosgyongyosi.cashflow.dto.TransactionViewDTO;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.dto.RateMetaDTO;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionRepository transactionRepository;
    private final TransactionCategoryRepository categoryRepository;
    private final FxService fxService;

    public TransactionController(TransactionRepository transactionRepository,
                                 TransactionCategoryRepository categoryRepository,
                                 FxService fxService) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.fxService = fxService;
    }
    
    @GetMapping
    public List<TransactionViewDTO> getAllTransactions(
            @RequestParam(name = "displayCurrency", required = false) String requestedCurrency) {
        List<Transaction> allTx = transactionRepository.findAll();
        log.debug("getAllTransactions() returning {} rows (requestedCurrency={})", allTx.size(), requestedCurrency);

        Currency targetCurrency = resolveDisplayCurrency(requestedCurrency);
        if (targetCurrency == null) {
            return allTx.stream().map(TransactionViewDTO::from).toList();
        }

        Function<Transaction, TransactionViewDTO> mapper = buildConversionMapper(targetCurrency);
        return allTx.stream().map(mapper).toList();
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

    private Currency resolveDisplayCurrency(String requested) {
        if (requested == null || requested.isBlank() || requested.equalsIgnoreCase("original")) {
            return null;
        }
        try {
            return Currency.valueOf(requested.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unsupported displayCurrency: " + requested);
        }
    }

    private Function<Transaction, TransactionViewDTO> buildConversionMapper(Currency targetCurrency) {
        Map<String, RateMetaDTO> metaCache = new HashMap<>();
        return tx -> {
            if (tx.getCurrency() == targetCurrency) {
                return TransactionViewDTO.from(tx);
            }

            LocalDate effectiveDate = tx.getValueDate() != null ? tx.getValueDate() : tx.getBookingDate();
            if (effectiveDate == null) {
                effectiveDate = LocalDate.now();
            }
            try {
                var result = fxService.convertWithDetails(tx.getAmount(), tx.getCurrency(), targetCurrency, effectiveDate);
                BigDecimal converted = result.convertedAmount().setScale(2, RoundingMode.HALF_UP);
                RateMetaDTO meta = result.baseToQuoteMeta();
                // Cache the metadata to avoid repeated object creation for identical pairs.
                if (meta != null) {
                    String key = meta.getQuoteCurrency() + "@" + meta.getRateDateUsed();
                    metaCache.putIfAbsent(key, meta);
                    meta = metaCache.get(key);
                }
                return TransactionViewDTO.withConversion(tx, converted, targetCurrency,
                        meta != null ? meta.getRateDateUsed() : null,
                        meta != null ? meta.getProvider() : null);
            } catch (Exception ex) {
                log.warn("FX conversion failed for transaction {} {} -> {}: {}", tx.getId(), tx.getCurrency(), targetCurrency, ex.getMessage());
                return TransactionViewDTO.from(tx);
            }
        };
    }
}
