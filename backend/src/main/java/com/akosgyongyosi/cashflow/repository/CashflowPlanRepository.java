package com.akosgyongyosi.cashflow.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;


@Repository
public interface CashflowPlanRepository extends JpaRepository<CashflowPlan, Long> {
}