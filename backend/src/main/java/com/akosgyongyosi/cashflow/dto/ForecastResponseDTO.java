package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class ForecastResponseDTO {
    private Long cashflowPlanId;
    private Map<Integer, BigDecimal> weeklyTotals;
    private List<ForecastTransactionDTO> forecastTransactions; 
}
