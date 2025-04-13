package com.akosgyongyosi.cashflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.akosgyongyosi.cashflow.entity.AccountingCategory;

public interface AccountingCategoryRepository extends JpaRepository<AccountingCategory, Long> {
    
}
