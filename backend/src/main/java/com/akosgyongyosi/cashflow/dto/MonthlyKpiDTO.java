package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class MonthlyKpiDTO {
    private int month;
    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal netCashFlow = BigDecimal.ZERO;
    private BigDecimal bankBalance = BigDecimal.ZERO;
    private Map<String, BigDecimal> incomeAccountingCategorySums = new HashMap<>();
    private Map<String, BigDecimal> expenseAccountingCategorySums = new HashMap<>();
    private Map<String, BigDecimal> accountingCategorySums = new HashMap<>();
    private Map<String, BigDecimal> transactionCategorySums = new HashMap<>();
    private Map<String, String> transactionCategoryDirections = new HashMap<>();
    private String rateDate; 
    private String rateSource; 
    private BigDecimal originalTotalIncome;
    private BigDecimal originalTotalExpense;
    private BigDecimal originalNetCashFlow;
    private BigDecimal originalBankBalance;
}
