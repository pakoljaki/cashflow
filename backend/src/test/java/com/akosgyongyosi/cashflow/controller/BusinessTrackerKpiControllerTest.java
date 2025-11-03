package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiDisplayCurrencyConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BusinessTrackerKpiControllerTest {

    @Mock
    private KpiCalculationService kpiService;

    @Mock
    private KpiDisplayCurrencyConverter displayConverter;

    @InjectMocks
    private BusinessTrackerKpiController businessTrackerKpiController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getBusinessKpi_shouldReturnKpiForPeriod() {
        String startDate = "2024-01-01";
        String endDate = "2024-12-31";
        BigDecimal startBalance = new BigDecimal("10000.00");
        Currency baseCurrency = Currency.HUF;

        KpiDashboardDTO expectedKpi = new KpiDashboardDTO();
        expectedKpi.setBaseCurrency("HUF");
        expectedKpi.setStartBalance(startBalance);
        expectedKpi.setTotalRevenue(new BigDecimal("50000.00"));
        expectedKpi.setTotalExpenses(new BigDecimal("30000.00"));

        when(kpiService.calculateForPeriod(
            eq(LocalDate.of(2024, 1, 1)),
            eq(LocalDate.of(2024, 12, 31)),
            eq(startBalance),
            eq(baseCurrency)
        )).thenReturn(expectedKpi);

        KpiDashboardDTO result = businessTrackerKpiController.getBusinessKpi(
            startDate, endDate, startBalance, baseCurrency, null);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("HUF");
        assertThat(result.getStartBalance()).isEqualByComparingTo(startBalance);
        verify(kpiService).calculateForPeriod(any(), any(), any(), eq(baseCurrency));
    }

    @Test
    void getBusinessKpi_shouldUseDefaultsWhenOptionalParamsNotProvided() {
        String startDate = "2024-01-01";
        String endDate = "2024-12-31";

        KpiDashboardDTO expectedKpi = new KpiDashboardDTO();
        expectedKpi.setBaseCurrency("HUF");
        expectedKpi.setStartBalance(BigDecimal.ZERO);

        when(kpiService.calculateForPeriod(
            any(LocalDate.class),
            any(LocalDate.class),
            eq(BigDecimal.ZERO),
            eq(Currency.HUF)
        )).thenReturn(expectedKpi);

        KpiDashboardDTO result = businessTrackerKpiController.getBusinessKpi(
            startDate, endDate, null, null, null);

        assertThat(result).isNotNull();
        verify(kpiService).calculateForPeriod(
            eq(LocalDate.of(2024, 1, 1)),
            eq(LocalDate.of(2024, 12, 31)),
            eq(BigDecimal.ZERO),
            eq(Currency.HUF)
        );
    }

    @Test
    void getBusinessKpi_shouldConvertToDisplayCurrencyWhenProvided() {
        String startDate = "2024-01-01";
        String endDate = "2024-12-31";
        BigDecimal startBalance = new BigDecimal("10000.00");
        Currency baseCurrency = Currency.HUF;
        Currency displayCurrency = Currency.EUR;

        KpiDashboardDTO baseCurrencyKpi = new KpiDashboardDTO();
        baseCurrencyKpi.setBaseCurrency("HUF");
        baseCurrencyKpi.setTotalRevenue(new BigDecimal("50000.00"));

        KpiDashboardDTO convertedKpi = new KpiDashboardDTO();
        convertedKpi.setBaseCurrency("EUR");
        convertedKpi.setTotalRevenue(new BigDecimal("130.00"));

        when(kpiService.calculateForPeriod(
            any(LocalDate.class),
            any(LocalDate.class),
            eq(startBalance),
            eq(baseCurrency)
        )).thenReturn(baseCurrencyKpi);

        when(displayConverter.toDisplayCurrency(
            eq(baseCurrencyKpi),
            eq(LocalDate.of(2024, 1, 1)),
            eq(baseCurrency),
            eq(displayCurrency)
        )).thenReturn(convertedKpi);

        KpiDashboardDTO result = businessTrackerKpiController.getBusinessKpi(
            startDate, endDate, startBalance, baseCurrency, displayCurrency);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("130.00");
        verify(displayConverter).toDisplayCurrency(
            eq(baseCurrencyKpi),
            eq(LocalDate.of(2024, 1, 1)),
            eq(baseCurrency),
            eq(displayCurrency)
        );
    }

    @Test
    void getBusinessKpi_shouldNotConvertWhenDisplayCurrencySameAsBase() {
        String startDate = "2024-01-01";
        String endDate = "2024-12-31";
        Currency baseCurrency = Currency.HUF;
        Currency displayCurrency = Currency.HUF;

        KpiDashboardDTO expectedKpi = new KpiDashboardDTO();
        expectedKpi.setBaseCurrency("HUF");

        when(kpiService.calculateForPeriod(
            any(LocalDate.class),
            any(LocalDate.class),
            any(BigDecimal.class),
            eq(baseCurrency)
        )).thenReturn(expectedKpi);

        KpiDashboardDTO result = businessTrackerKpiController.getBusinessKpi(
            startDate, endDate, null, baseCurrency, displayCurrency);

        assertThat(result).isNotNull();
        verify(displayConverter, never()).toDisplayCurrency(any(), any(), any(), any());
    }
}
