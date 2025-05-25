package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class ForecastTransactionDTO {
    private String category;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private boolean isAssumptionBased;
}

