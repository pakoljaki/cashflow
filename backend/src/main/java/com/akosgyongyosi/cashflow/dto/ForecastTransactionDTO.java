package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ForecastTransactionDTO {
    
    private String category;
    
    private BigDecimal amount;
    
    private LocalDate transactionDate;

    private boolean isAssumptionBased; // True if this is from an assumption, false if from historical data
}

