package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ForecastResponseDTO {
    
    private Long cashflowPlanId;
    
    private Map<Integer, BigDecimal> weeklyTotals; // key=Week Number, value=Sum

    private List<ForecastTransactionDTO> forecastTransactions; 
}
