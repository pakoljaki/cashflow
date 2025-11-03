package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.CreatePlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.ScenarioPlanRequestDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ScenarioType;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.service.CashflowPlanService;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CashflowPlanControllerTest {

    private CashflowPlanService planService;
    private KpiCalculationService kpiService;
    private CashflowPlanRepository planRepository;
    private CashflowPlanController controller;

    @BeforeEach
    void setUp() {
        planService = mock(CashflowPlanService.class);
        kpiService = mock(KpiCalculationService.class);
        planRepository = mock(CashflowPlanRepository.class);
        controller = new CashflowPlanController(planService, kpiService, planRepository);
    }

    @Test
    void createPlanForCurrentYear_creates_plan_with_current_year_dates() {
        CashflowPlan plan = new CashflowPlan();
        plan.setId(1L);
        plan.setPlanName("2025 Plan");
        when(planService.createPlanForInterval(anyString(), any(LocalDate.class), any(LocalDate.class), anyString()))
                .thenReturn(plan);
        when(planRepository.save(any(CashflowPlan.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.createPlanForCurrentYear("2025 Plan", Currency.HUF);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBaseCurrency()).isEqualTo(Currency.HUF);
        verify(planRepository).save(any(CashflowPlan.class));
    }

    @Test
    void createPlanForCurrentYear_defaults_to_HUF_when_no_currency_specified() {
        CashflowPlan plan = new CashflowPlan();
        when(planService.createPlanForInterval(anyString(), any(), any(), anyString())).thenReturn(plan);
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.createPlanForCurrentYear("Plan", null);

        assertThat(response.getBody().getBaseCurrency()).isEqualTo(Currency.HUF);
    }

    @Test
    void createPlanForInterval_creates_plan_with_specified_dates() {
        CreatePlanRequestDTO request = new CreatePlanRequestDTO();
        request.setPlanName("Q1 Plan");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 3, 31));
        request.setBaseCurrency(Currency.EUR);
        request.setStartBalance(BigDecimal.valueOf(10000));

        CashflowPlan plan = new CashflowPlan();
        plan.setId(1L);
        when(planService.createPlanForInterval(anyString(), any(), any(), anyString())).thenReturn(plan);
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.createPlanForInterval(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        verify(planService).createPlanForInterval(eq("Q1 Plan"), any(), any(), anyString());
    }

    @Test
    void createPlanForInterval_returns_bad_request_when_name_is_blank() {
        CreatePlanRequestDTO request = new CreatePlanRequestDTO();
        request.setPlanName("");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 12, 31));

        var response = controller.createPlanForInterval(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        verify(planService, never()).createPlanForInterval(anyString(), any(), any(), anyString());
    }

    @Test
    void createPlanForInterval_returns_bad_request_when_start_after_end() {
        CreatePlanRequestDTO request = new CreatePlanRequestDTO();
        request.setPlanName("Invalid Plan");
        request.setStartDate(LocalDate.of(2025, 12, 31));
        request.setEndDate(LocalDate.of(2025, 1, 1));

        var response = controller.createPlanForInterval(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void getAllPlans_returns_all_plans() {
        List<CashflowPlan> plans = List.of(new CashflowPlan(), new CashflowPlan());
        when(planService.findAll()).thenReturn(plans);

        var response = controller.getAllPlans();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getPlan_returns_plan_when_found() {
        CashflowPlan plan = new CashflowPlan();
        plan.setId(1L);
        when(planService.getPlan(1L)).thenReturn(Optional.of(plan));

        var response = controller.getPlan(1L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(plan);
    }

    @Test
    void getPlan_returns_not_found_when_plan_does_not_exist() {
        when(planService.getPlan(999L)).thenReturn(Optional.empty());

        var response = controller.getPlan(999L);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void getPlansForGroup_returns_plans_for_group() {
        CashflowPlan plan1 = new CashflowPlan();
        plan1.setGroupKey("group-123");
        CashflowPlan plan2 = new CashflowPlan();
        plan2.setGroupKey("group-123");
        when(planService.findAllByGroupKey("group-123")).thenReturn(List.of(plan1, plan2));

        var response = controller.getPlansForGroup("group-123");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getPlansForGroup_returns_not_found_when_no_plans_exist() {
        when(planService.findAllByGroupKey("nonexistent")).thenReturn(List.of());

        var response = controller.getPlansForGroup("nonexistent");

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void createScenarioPlans_creates_three_plans() {
        ScenarioPlanRequestDTO request = new ScenarioPlanRequestDTO();
        request.setBasePlanName("2025 Scenarios");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 12, 31));
        request.setStartBalance(BigDecimal.valueOf(50000));
        request.setBaseCurrency(Currency.EUR);

        CashflowPlan worst = new CashflowPlan();
        worst.setScenario(ScenarioType.WORST);
        CashflowPlan realistic = new CashflowPlan();
        realistic.setScenario(ScenarioType.REALISTIC);
        CashflowPlan best = new CashflowPlan();
        best.setScenario(ScenarioType.BEST);

        when(planService.createAllScenarioPlans(anyString(), any(), any(), any()))
                .thenReturn(List.of(worst, realistic, best));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller.createScenarioPlans(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(3);
        verify(planRepository, times(3)).save(any(CashflowPlan.class));
    }

    @Test
    void createScenarioPlans_returns_bad_request_when_start_after_end() {
        ScenarioPlanRequestDTO request = new ScenarioPlanRequestDTO();
        request.setStartDate(LocalDate.of(2025, 12, 31));
        request.setEndDate(LocalDate.of(2025, 1, 1));

        var response = controller.createScenarioPlans(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void getMonthlyKpi_returns_monthly_kpi_data() {
        KpiDashboardDTO dashboard = new KpiDashboardDTO();
        List<MonthlyKpiDTO> monthlyData = new ArrayList<>();
        MonthlyKpiDTO jan = new MonthlyKpiDTO();
        jan.setMonth(1);
        monthlyData.add(jan);
        dashboard.setMonthlyData(monthlyData);

        when(kpiService.calculateForPlan(1L)).thenReturn(dashboard);

        var response = controller.getMonthlyKpi(1L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getMonth()).isEqualTo(1);
    }

    @Test
    void getMonthlyKpi_returns_not_found_when_plan_does_not_exist() {
        when(kpiService.calculateForPlan(999L)).thenReturn(null);

        var response = controller.getMonthlyKpi(999L);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    void deletePlanGroup_deletes_group_and_returns_no_content() {
        when(planService.deletePlanGroup("group-123")).thenReturn(true);

        var response = controller.deletePlanGroup("group-123");

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(planService).deletePlanGroup("group-123");
    }

    @Test
    void deletePlanGroup_returns_not_found_when_group_does_not_exist() {
        when(planService.deletePlanGroup("nonexistent")).thenReturn(false);

        var response = controller.deletePlanGroup("nonexistent");

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }
}
