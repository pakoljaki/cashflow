package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiDisplayCurrencyConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KpiDashboardControllerTest {

    @Mock
    private KpiCalculationService kpiService;

    @Mock
    private CashflowPlanRepository planRepository;

    @Mock
    private CashflowCalculationService cashflowCalculationService;

    @Mock
    private KpiDisplayCurrencyConverter displayConverter;

    @InjectMocks
    private KpiDashboardController kpiDashboardController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getKpi_shouldReturnKpiDashboardWithBaseCurrency() {
        Long planId = 1L;
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        plan.setStartDate(LocalDate.of(2024, 1, 1));

        KpiDashboardDTO kpiDashboard = new KpiDashboardDTO();
        kpiDashboard.setBaseCurrency("USD");
        kpiDashboard.setTotalRevenue(new java.math.BigDecimal("10000.0"));
        kpiDashboard.setTotalExpenses(new java.math.BigDecimal("5000.0"));

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);
        when(kpiService.calculateForPlan(planId)).thenReturn(kpiDashboard);

        KpiDashboardDTO result = kpiDashboardController.getKpi(planId, null);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("USD");
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("10000.0");
        verify(planRepository).findById(planId);
        verify(cashflowCalculationService).applyAllAssumptions(plan);
        verify(kpiService).calculateForPlan(planId);
        verify(planRepository).save(plan);
    }

    @Test
    void getKpi_shouldConvertToDisplayCurrencyWhenProvided() {
        Long planId = 1L;
        Currency displayCurrency = Currency.EUR;
        
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        plan.setStartDate(LocalDate.of(2024, 1, 1));

        KpiDashboardDTO baseCurrencyKpi = new KpiDashboardDTO();
        baseCurrencyKpi.setBaseCurrency("USD");
        baseCurrencyKpi.setTotalRevenue(new java.math.BigDecimal("10000.0"));

        KpiDashboardDTO convertedKpi = new KpiDashboardDTO();
        convertedKpi.setBaseCurrency("EUR");
        convertedKpi.setTotalRevenue(new java.math.BigDecimal("9000.0"));

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);
        when(kpiService.calculateForPlan(planId)).thenReturn(baseCurrencyKpi);
        when(displayConverter.toDisplayCurrency(baseCurrencyKpi, plan.getStartDate(), 
            Currency.USD, Currency.EUR)).thenReturn(convertedKpi);

        KpiDashboardDTO result = kpiDashboardController.getKpi(planId, displayCurrency);

        assertThat(result).isNotNull();
        assertThat(result.getBaseCurrency()).isEqualTo("EUR");
        assertThat(result.getTotalRevenue()).isEqualByComparingTo("9000.0");
        verify(displayConverter).toDisplayCurrency(baseCurrencyKpi, plan.getStartDate(), 
            Currency.USD, Currency.EUR);
    }

    @Test
    void getKpi_shouldSetBaseCurrencyIfNull() {
        Long planId = 1L;
        CashflowPlan plan = new CashflowPlan();
        plan.setId(planId);
        plan.setBaseCurrency(Currency.USD);
        plan.setStartDate(LocalDate.of(2024, 1, 1));

        KpiDashboardDTO kpiDashboard = new KpiDashboardDTO();
        kpiDashboard.setTotalRevenue(new java.math.BigDecimal("10000.0"));

        when(planRepository.findById(planId)).thenReturn(Optional.of(plan));
        doNothing().when(cashflowCalculationService).applyAllAssumptions(plan);
        when(kpiService.calculateForPlan(planId)).thenReturn(kpiDashboard);

        KpiDashboardDTO result = kpiDashboardController.getKpi(planId, null);

        assertThat(result.getBaseCurrency()).isEqualTo("USD");
    }

    @Test
    void getKpi_shouldThrowExceptionWhenPlanNotFound() {
        Long planId = 999L;
        when(planRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kpiDashboardController.getKpi(planId, null))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Plan not found");
        
        verify(planRepository).findById(planId);
        verify(cashflowCalculationService, never()).applyAllAssumptions(any());
        verify(kpiService, never()).calculateForPlan(any());
    }
}
