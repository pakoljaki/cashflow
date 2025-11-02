package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.FxSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxSettingsRepository extends JpaRepository<FxSettings, Long> {
    Optional<FxSettings> findTopByOrderByIdAsc();
}
