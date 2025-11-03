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
    // FX metadata for dashboard-level conversions
    private String baseCurrency; // canonical/base currency
    private String displayCurrency; // target display currency if conversion performed
    private String startBalanceRateDate; // rate date used for start balance conversion
    private String startBalanceRateSource; // provider for start balance conversion
    private BigDecimal originalStartBalance; // original base start balance for dual display
}
