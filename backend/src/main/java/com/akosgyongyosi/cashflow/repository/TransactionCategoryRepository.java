package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import java.util.Optional;
import java.util.List;


@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, Long> {
    Optional<TransactionCategory> findByName(String name);
    List<TransactionCategory> findByDirection(TransactionDirection direction);
}
