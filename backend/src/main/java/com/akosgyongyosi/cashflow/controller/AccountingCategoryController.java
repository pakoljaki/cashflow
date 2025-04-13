package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.UpdateCategoryMappingRequestDTO;
import com.akosgyongyosi.cashflow.entity.AccountingCategory;
import com.akosgyongyosi.cashflow.service.AccountingCategoryService;
import com.akosgyongyosi.cashflow.repository.AccountingCategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting-categories")
public class AccountingCategoryController {

    private final AccountingCategoryService accountingCategoryService;
    private final AccountingCategoryRepository accountingCategoryRepository;

    public AccountingCategoryController(AccountingCategoryService accountingCategoryService,
                                        AccountingCategoryRepository accountingCategoryRepository) {
        this.accountingCategoryService = accountingCategoryService;
        this.accountingCategoryRepository = accountingCategoryRepository;
    }

    @GetMapping
    public List<AccountingCategory> getAllAccountingCategories() {
        return accountingCategoryService.getAllAccountingCategories();
    }

    @PostMapping
    public ResponseEntity<?> createAccountingCategory(@RequestBody AccountingCategory category) {
        AccountingCategory saved = accountingCategoryRepository.save(category);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/mapping")
    public ResponseEntity<?> updateMapping(@RequestBody UpdateCategoryMappingRequestDTO request) {
        if (request.getAccountingCategoryId() == null || request.getTransactionCategoryIds() == null) {
            return ResponseEntity.badRequest().body("Missing accountingCategoryId or transactionCategoryIds");
        }
        accountingCategoryService.updateTransactionCategoryMapping(request.getAccountingCategoryId(), request.getTransactionCategoryIds());
        return ResponseEntity.ok().build();
    }
}
