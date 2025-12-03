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
    private int windowDays;      
    private int sampleSize;     
    private BigDecimal mean;     
    private BigDecimal stdDev;   
    private BigDecimal min;
    private BigDecimal max;
    private boolean partial;
}
