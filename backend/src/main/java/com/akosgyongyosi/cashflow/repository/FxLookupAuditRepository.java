package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.FxLookupAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxLookupAuditRepository extends JpaRepository<FxLookupAudit, Long> {
}
