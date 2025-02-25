package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanLineItemRepository extends JpaRepository<PlanLineItem, Long> {
    List<PlanLineItem> findByPlanId(Long planId);
}
