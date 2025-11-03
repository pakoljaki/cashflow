package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KpiDisplayCurrencyConverterTest {

    private FxService fxService;
    private KpiDisplayCurrencyConverter converter;

    @BeforeEach
    void setUp() {
        fxService = mock(FxService.class);
        converter = new KpiDisplayCurrencyConverter(fxService);
    }

    @Test
    void toDisplayCurrency_shouldReturnOriginalWhenDisplayCurrencyIsNull() {
        KpiDashboardDTO dto = createTestDashboard();
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, null);

        assertThat(result).isSameAs(dto);
        verify(fxService, never()).convert(any(), any(), any(), any());
    }

    @Test
    void toDisplayCurrency_shouldReturnOriginalWhenDisplayCurrencyEqualsBase() {
        KpiDashboardDTO dto = createTestDashboard();
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.EUR, Currency.EUR);

        assertThat(result).isSameAs(dto);
        verify(fxService, never()).convert(any(), any(), any(), any());
    }

    @Test
    void toDisplayCurrency_shouldConvertStartBalance() {
        KpiDashboardDTO dto = createTestDashboard();
        dto.setStartBalance(new BigDecimal("100000"));
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        // Mock rate: 1 HUF = 0.0025 EUR (rate of 0.0025)
        when(fxService.convert(eq(BigDecimal.ONE), eq(Currency.HUF), eq(Currency.EUR), any()))
            .thenReturn(new BigDecimal("0.0025"));

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, Currency.EUR);

        assertThat(result.getStartBalance()).isEqualByComparingTo("250"); // 100000 * 0.0025
        assertThat(result.getOriginalStartBalance()).isEqualByComparingTo("100000");
        assertThat(result.getBaseCurrency()).isEqualTo("HUF");
        assertThat(result.getDisplayCurrency()).isEqualTo("EUR");
        assertThat(result.getStartBalanceRateDate()).isEqualTo("2023-12-31");
        assertThat(result.getStartBalanceRateSource()).isEqualTo("ECB/Frankfurter");
    }

    @Test
    void toDisplayCurrency_shouldConvertMonthlyDataFields() {
        KpiDashboardDTO dto = createTestDashboard();
        MonthlyKpiDTO monthlyData = new MonthlyKpiDTO();
        monthlyData.setMonth(1);
        monthlyData.setTotalIncome(new BigDecimal("500000"));
        monthlyData.setTotalExpense(new BigDecimal("300000"));
        monthlyData.setNetCashFlow(new BigDecimal("200000"));
        monthlyData.setBankBalance(new BigDecimal("800000"));
        monthlyData.setAccountingCategorySums(new HashMap<>());
        monthlyData.setTransactionCategorySums(new HashMap<>());
        dto.setMonthlyData(List.of(monthlyData));

        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        // Mock rate: 1 HUF = 0.0025 EUR
        when(fxService.convert(eq(BigDecimal.ONE), eq(Currency.HUF), eq(Currency.EUR), any()))
            .thenReturn(new BigDecimal("0.0025"));

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, Currency.EUR);

        MonthlyKpiDTO convertedMonth = result.getMonthlyData().get(0);
        assertThat(convertedMonth.getTotalIncome()).isEqualByComparingTo("1250");
        assertThat(convertedMonth.getTotalExpense()).isEqualByComparingTo("750");
        assertThat(convertedMonth.getNetCashFlow()).isEqualByComparingTo("500");
        assertThat(convertedMonth.getBankBalance()).isEqualByComparingTo("2000");

        assertThat(convertedMonth.getOriginalTotalIncome()).isEqualByComparingTo("500000");
        assertThat(convertedMonth.getOriginalTotalExpense()).isEqualByComparingTo("300000");
        assertThat(convertedMonth.getOriginalNetCashFlow()).isEqualByComparingTo("200000");
        assertThat(convertedMonth.getOriginalBankBalance()).isEqualByComparingTo("800000");

        assertThat(convertedMonth.getRateDate()).isEqualTo("2024-01-31");
        assertThat(convertedMonth.getRateSource()).isEqualTo("ECB/Frankfurter");
    }

    @Test
    void toDisplayCurrency_shouldConvertAccountingCategorySums() {
        KpiDashboardDTO dto = createTestDashboard();
        MonthlyKpiDTO monthlyData = new MonthlyKpiDTO();
        monthlyData.setMonth(3);
        
        Map<String, BigDecimal> accountingSums = new HashMap<>();
        accountingSums.put("Revenue", new BigDecimal("1000000"));
        accountingSums.put("Operating Expenses", new BigDecimal("400000"));
        monthlyData.setAccountingCategorySums(accountingSums);
        monthlyData.setTransactionCategorySums(new HashMap<>());
        
        dto.setMonthlyData(List.of(monthlyData));
        LocalDate periodStart = LocalDate.of(2024, 3, 1);

        // Mock rate: 1 HUF = 0.003 USD
        when(fxService.convert(eq(BigDecimal.ONE), eq(Currency.HUF), eq(Currency.USD), any()))
            .thenReturn(new BigDecimal("0.003"));

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, Currency.USD);

        Map<String, BigDecimal> convertedSums = result.getMonthlyData().get(0).getAccountingCategorySums();
        assertThat(convertedSums.get("Revenue")).isEqualByComparingTo("3000");
        assertThat(convertedSums.get("Operating Expenses")).isEqualByComparingTo("1200");
    }

    @Test
    void toDisplayCurrency_shouldConvertTransactionCategorySums() {
        KpiDashboardDTO dto = createTestDashboard();
        MonthlyKpiDTO monthlyData = new MonthlyKpiDTO();
        monthlyData.setMonth(6);
        
        Map<String, BigDecimal> transactionSums = new HashMap<>();
        transactionSums.put("Salary", new BigDecimal("600000"));
        transactionSums.put("Rent", new BigDecimal("120000"));
        transactionSums.put("Utilities", new BigDecimal("30000"));
        monthlyData.setTransactionCategorySums(transactionSums);
        monthlyData.setAccountingCategorySums(new HashMap<>());
        
        dto.setMonthlyData(List.of(monthlyData));
        LocalDate periodStart = LocalDate.of(2024, 6, 1);

        when(fxService.convert(any(BigDecimal.class), eq(Currency.EUR), eq(Currency.USD), any()))
            .thenAnswer(inv -> {
                BigDecimal amount = inv.getArgument(0);
                return amount.multiply(new BigDecimal("1.1"));
            });

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.EUR, Currency.USD);

        Map<String, BigDecimal> convertedSums = result.getMonthlyData().get(0).getTransactionCategorySums();
        assertThat(convertedSums.get("Salary")).isEqualByComparingTo("660000");
        assertThat(convertedSums.get("Rent")).isEqualByComparingTo("132000");
        assertThat(convertedSums.get("Utilities")).isEqualByComparingTo("33000");
    }

    @Test
    void toDisplayCurrency_shouldRecalculateTotalRevenueAndExpenses() {
        KpiDashboardDTO dto = createTestDashboard();
        
        MonthlyKpiDTO month1 = new MonthlyKpiDTO();
        month1.setMonth(1);
        month1.setTotalIncome(new BigDecimal("400000"));
        month1.setTotalExpense(new BigDecimal("200000"));
        month1.setAccountingCategorySums(new HashMap<>());
        month1.setTransactionCategorySums(new HashMap<>());
        
        MonthlyKpiDTO month2 = new MonthlyKpiDTO();
        month2.setMonth(2);
        month2.setTotalIncome(new BigDecimal("500000"));
        month2.setTotalExpense(new BigDecimal("300000"));
        month2.setAccountingCategorySums(new HashMap<>());
        month2.setTransactionCategorySums(new HashMap<>());
        
        dto.setMonthlyData(List.of(month1, month2));
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        // Mock rate: 1 HUF = 0.0025 EUR
        when(fxService.convert(eq(BigDecimal.ONE), eq(Currency.HUF), eq(Currency.EUR), any()))
            .thenReturn(new BigDecimal("0.0025"));

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, Currency.EUR);

        // month1: income 1000, expense 500
        // month2: income 1250, expense 750
        // total: income 2250, expense 1250
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("2250");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("1250");
    }

    @Test
    void toDisplayCurrency_shouldHandleMultipleMonthsAcrossDifferentYears() {
        KpiDashboardDTO dto = createTestDashboard();
        
        MonthlyKpiDTO month11 = new MonthlyKpiDTO();
        month11.setMonth(11);
        month11.setTotalIncome(new BigDecimal("100000"));
        month11.setTotalExpense(new BigDecimal("50000"));
        month11.setAccountingCategorySums(new HashMap<>());
        month11.setTransactionCategorySums(new HashMap<>());
        
        MonthlyKpiDTO month12 = new MonthlyKpiDTO();
        month12.setMonth(12);
        month12.setTotalIncome(new BigDecimal("150000"));
        month12.setTotalExpense(new BigDecimal("75000"));
        month12.setAccountingCategorySums(new HashMap<>());
        month12.setTransactionCategorySums(new HashMap<>());
        
        dto.setMonthlyData(List.of(month11, month12));
        LocalDate periodStart = LocalDate.of(2024, 11, 1);

        when(fxService.convert(any(BigDecimal.class), any(), any(), any()))
            .thenAnswer(inv -> inv.getArgument(0)); // 1:1 conversion for simplicity

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.USD, Currency.EUR);

        assertThat(result.getMonthlyData()).hasSize(2);
        assertThat(result.getMonthlyData().get(0).getRateDate()).isEqualTo("2024-11-30");
        assertThat(result.getMonthlyData().get(1).getRateDate()).isEqualTo("2024-12-31");
    }

    @Test
    void toDisplayCurrency_shouldHandleEmptyMonthlyData() {
        KpiDashboardDTO dto = createTestDashboard();
        dto.setMonthlyData(Collections.emptyList());
        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        when(fxService.convert(any(), any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, Currency.EUR);

        assertThat(result.getMonthlyData()).isEmpty();
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(result.getTotalExpenses()).isEqualByComparingTo("0");
    }

    @Test
    void toDisplayCurrency_shouldHandleEmptyMaps() {
        KpiDashboardDTO dto = createTestDashboard();
        MonthlyKpiDTO monthlyData = new MonthlyKpiDTO();
        monthlyData.setMonth(1);
        monthlyData.setAccountingCategorySums(Collections.emptyMap());
        monthlyData.setTransactionCategorySums(Collections.emptyMap());
        dto.setMonthlyData(List.of(monthlyData));

        LocalDate periodStart = LocalDate.of(2024, 1, 1);

        when(fxService.convert(any(), any(), any(), any()))
            .thenReturn(BigDecimal.ZERO);

        KpiDashboardDTO result = converter.toDisplayCurrency(dto, periodStart, Currency.HUF, Currency.EUR);

        assertThat(result.getMonthlyData().get(0).getAccountingCategorySums()).isEmpty();
        assertThat(result.getMonthlyData().get(0).getTransactionCategorySums()).isEmpty();
    }

    private KpiDashboardDTO createTestDashboard() {
        KpiDashboardDTO dto = new KpiDashboardDTO();
        dto.setStartBalance(BigDecimal.ZERO);
        dto.setMonthlyData(new ArrayList<>());
        return dto;
    }
}
