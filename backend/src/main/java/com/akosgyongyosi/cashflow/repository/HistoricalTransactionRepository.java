package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HistoricalTransactionRepository extends JpaRepository<HistoricalTransaction, Long> {
    List<HistoricalTransaction> findByCashflowPlanId(Long cashflowPlanId);
    List<HistoricalTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
}
