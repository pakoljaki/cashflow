package com.akosgyongyosi.cashflow.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;


@Repository
public interface CashflowPlanRepository extends JpaRepository<CashflowPlan, Long> {
    List<CashflowPlan> findByGroupKey(String groupKey);
}