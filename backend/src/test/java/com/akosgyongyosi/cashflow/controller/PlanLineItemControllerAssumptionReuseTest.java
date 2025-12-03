package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.PlanLineItemRequestDTO;
import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.PlanLineItemRepository;
import com.akosgyongyosi.cashflow.repository.TransactionCategoryRepository;
import com.akosgyongyosi.cashflow.service.AssumptionIdGeneratorService;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.forecast.CashflowCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@SuppressWarnings({"null"})
class PlanLineItemControllerAssumptionReuseTest {

    @Mock private CashflowPlanRepository planRepository;
    @Mock private PlanLineItemRepository lineItemRepository;
    @Mock private TransactionCategoryRepository categoryRepository;
    @Mock private AssumptionIdGeneratorService assumptionIdGenService;
    @Mock private CashflowCalculationService cashflowCalculationService;
    @Mock private com.akosgyongyosi.cashflow.service.fx.RateLookupService rateLookupService;
    @Mock private com.akosgyongyosi.cashflow.service.fx.TransactionDateRangeFxService transactionDateRangeFxService;
    @Mock private com.akosgyongyosi.cashflow.service.CashflowPlanService cashflowPlanService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private PlanLineItemController controller;

    private CashflowPlan planA;
    private CashflowPlan planB;
    private Principal principal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@example.com");
        planA = new CashflowPlan(); planA.setId(100L); planA.setBaseCurrency(Currency.HUF); planA.setLineItems(new java.util.ArrayList<>());
        planB = new CashflowPlan(); planB.setId(200L); planB.setBaseCurrency(Currency.HUF); planB.setLineItems(new java.util.ArrayList<>());
    }

    private PlanLineItemRequestDTO buildRecurringDto() {
        PlanLineItemRequestDTO dto = new PlanLineItemRequestDTO();
        dto.setTitle("Shared EUR Revenue");
        dto.setType(LineItemType.RECURRING);
        dto.setAmount(new BigDecimal("3000"));
        dto.setCurrency(Currency.EUR);
        dto.setFrequency(Frequency.MONTHLY);
        dto.setStartDate(LocalDate.of(2025, 1, 1));
        dto.setEndDate(LocalDate.of(2025, 12, 31));
        return dto;
    }

    @Test
    void recurringLineItem_secondPlan_reusesAssumptionIdFromFirstPlan() {
        PlanLineItemRequestDTO dto1 = buildRecurringDto();
        PlanLineItem savedFirst = new PlanLineItem();
        savedFirst.setId(1L);
        savedFirst.setTitle(dto1.getTitle());
        savedFirst.setType(LineItemType.RECURRING);
        savedFirst.setAssumptionId(9000L); 

        when(planRepository.findById(planA.getId())).thenReturn(Optional.of(planA));
        when(lineItemRepository.findFirstByTitleIgnoreCaseAndTypeAndStartDateAndFrequency(
                dto1.getTitle(), dto1.getType(), dto1.getStartDate(), dto1.getFrequency())).thenReturn(null);
        when(assumptionIdGenService.getNextAssumptionId()).thenReturn(9000L);
        when(lineItemRepository.save(any(PlanLineItem.class))).thenReturn(savedFirst);
        doNothing().when(cashflowCalculationService).applyAllAssumptions(planA);

        ResponseEntity<?> resp1 = controller.createLineItem(planA.getId(), dto1, principal);
        assertThat(resp1.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(savedFirst.getAssumptionId()).isEqualTo(9000L);

        when(lineItemRepository.findFirstByTitleIgnoreCaseAndTypeAndStartDateAndFrequency(
                dto1.getTitle(), dto1.getType(), dto1.getStartDate(), dto1.getFrequency())).thenReturn(savedFirst);

        PlanLineItemRequestDTO dto2 = buildRecurringDto();
        PlanLineItem savedSecond = new PlanLineItem();
        savedSecond.setId(2L);
        savedSecond.setTitle(dto2.getTitle());
        savedSecond.setType(LineItemType.RECURRING);
        savedSecond.setAssumptionId(9000L);

        when(planRepository.findById(planB.getId())).thenReturn(Optional.of(planB));
        when(lineItemRepository.save(any(PlanLineItem.class))).thenAnswer(invocation -> {
            PlanLineItem arg = invocation.getArgument(0);
            assertThat(arg.getAssumptionId()).isEqualTo(9000L);
            return savedSecond;
        });
        doNothing().when(cashflowCalculationService).applyAllAssumptions(planB);

        ResponseEntity<?> resp2 = controller.createLineItem(planB.getId(), dto2, principal);
        assertThat(resp2.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(savedSecond.getAssumptionId()).isEqualTo(9000L);

        verify(assumptionIdGenService, times(1)).getNextAssumptionId();
        verify(lineItemRepository, times(2)).findFirstByTitleIgnoreCaseAndTypeAndStartDateAndFrequency(
                dto1.getTitle(), dto1.getType(), dto1.getStartDate(), dto1.getFrequency());
    }
}
