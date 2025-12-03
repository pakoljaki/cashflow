package com.akosgyongyosi.cashflow.dto;
import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter 
@Getter
public class KpiDashboardDTO {
    private BigDecimal startBalance;
    private List<MonthlyKpiDTO> monthlyData;
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal ebitda = BigDecimal.ZERO;
    private BigDecimal ebit = BigDecimal.ZERO;
    private BigDecimal profitMargin = BigDecimal.ZERO;
    private String baseCurrency; 
    private String balanceCurrency; 
    private String displayCurrency;
    private String startBalanceRateDate; 
    private String startBalanceRateSource;
    private BigDecimal originalStartBalance; 
}
