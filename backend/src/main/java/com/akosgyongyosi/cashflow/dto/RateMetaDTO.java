package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detailed metadata about the FX rate used for a conversion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateMetaDTO {
    private LocalDate rateDateUsed;     // The date whose rate was actually applied
    private Currency baseCurrency;      // Canonical base (e.g. EUR)
    private Currency quoteCurrency;     // Quote (target) currency
    private BigDecimal rate;            // Base -> Quote rate
    private boolean provisional;        // True if provisional (future-date fallback)
    private String provider;            // Source provider name
}
