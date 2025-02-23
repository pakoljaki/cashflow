package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
    Optional<TransactionCategory> findByName(String name);
}
