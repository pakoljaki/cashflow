package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxVolatilityDTO {
    private Currency base;
    private Currency quote;
    private int windowDays;      // requested window size
    private int sampleSize;      // actual number of observations
    private BigDecimal mean;     // arithmetic mean
    private BigDecimal stdDev;   // sample standard deviation (n-1)
    private BigDecimal min;
    private BigDecimal max;
    private boolean partial;     // true if sampleSize < windowDays
}
