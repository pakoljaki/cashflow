package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RateMetaDTO {
    private LocalDate rateDateUsed;     
    private Currency baseCurrency;     
    private Currency quoteCurrency;     
    private BigDecimal rate;          
    private boolean provisional;       
    private String provider;           
}
