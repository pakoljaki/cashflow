package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import com.akosgyongyosi.cashflow.entity.LineItemType;

@Repository
public interface PlanLineItemRepository extends JpaRepository<PlanLineItem, Long> {
    List<PlanLineItem> findByPlanId(Long planId);
    PlanLineItem findByPlanIdAndAssumptionId(Long planId, Long assumptionId);
    PlanLineItem findFirstByTitleIgnoreCaseAndTypeAndTransactionDate(String title, LineItemType type, LocalDate transactionDate);
    PlanLineItem findFirstByTitleIgnoreCaseAndTypeAndStartDateAndFrequency(String title, LineItemType type, LocalDate startDate, com.akosgyongyosi.cashflow.entity.Frequency frequency);
    PlanLineItem findFirstByTitleIgnoreCaseAndTypeAndStartDateAndPercentChange(String title, LineItemType type, LocalDate startDate, Double percentChange);
}
