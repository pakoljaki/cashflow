package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.AccountingCategory;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.repository.AccountingCategoryRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AccountingCategoryService {

    private final AccountingCategoryRepository accountingCategoryRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;

    public AccountingCategoryService(AccountingCategoryRepository accountingCategoryRepository,
                                     TransactionCategoryRepository transactionCategoryRepository) {
        this.accountingCategoryRepository = accountingCategoryRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
    }

    public List<AccountingCategory> getAllAccountingCategories() {
        return accountingCategoryRepository.findAll();
    }

    @Transactional
    public void updateTransactionCategoryMapping(Long accountingCategoryId, List<Long> transactionCategoryIds) {
        Optional<AccountingCategory> optionalAccountingCategory = accountingCategoryRepository.findById(accountingCategoryId);
        if (optionalAccountingCategory.isEmpty()) {
            throw new RuntimeException("Accounting Category not found");
        }

        AccountingCategory accountingCategory = optionalAccountingCategory.get();

        for (Long transactionCategoryId : transactionCategoryIds) {
            Optional<TransactionCategory> optionalTransactionCategory = transactionCategoryRepository.findById(transactionCategoryId);
            if (optionalTransactionCategory.isPresent()) {
                TransactionCategory transactionCategory = optionalTransactionCategory.get();
                transactionCategory.setAccountingCategory(accountingCategory);
                transactionCategoryRepository.save(transactionCategory);
            }
        }
    }
}
