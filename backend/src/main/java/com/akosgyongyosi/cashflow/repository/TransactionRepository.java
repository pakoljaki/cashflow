package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    //mivel a jpa-ba van csomo auto implemented method ide nem is kell, egyenlore
}
