package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTransactionDirection(String direction);
    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category")
    List<Transaction> findAllWithCategory();
}
