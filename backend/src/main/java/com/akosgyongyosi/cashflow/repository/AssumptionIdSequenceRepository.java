package com.akosgyongyosi.cashflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.akosgyongyosi.cashflow.entity.AssumptionIdSequence;

public interface AssumptionIdSequenceRepository extends JpaRepository<AssumptionIdSequence, String> {
}
