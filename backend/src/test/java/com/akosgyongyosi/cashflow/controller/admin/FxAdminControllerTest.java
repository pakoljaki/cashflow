package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.service.AuditLogService;
import com.akosgyongyosi.cashflow.service.fx.FxRefreshService;
import com.akosgyongyosi.cashflow.service.fx.FxSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxAdminControllerTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private FxSettingsService fxSettingsService;

    @Mock
    private AuditLogService auditLogService;

    private FxAdminController controller;

    @Mock
    private FxProperties fxProperties;

    @Mock
    private FxRefreshService fxRefreshService;

    @BeforeEach
    void setUp() {
        controller = new FxAdminController(exchangeRateRepository, fxSettingsService, auditLogService, fxProperties, fxRefreshService);
    }

    @Test
    void health_shouldReturnLatestRateDatesForAllCurrencies() {
        LocalDate hufDate = LocalDate.of(2025, 11, 1);
        LocalDate usdDate = LocalDate.of(2025, 11, 2);
        LocalDate eurDate = LocalDate.of(2025, 11, 3);

        ExchangeRate hufRate = new ExchangeRate();
        hufRate.setRateDate(hufDate);
        
        ExchangeRate usdRate = new ExchangeRate();
        usdRate.setRateDate(usdDate);
        
        ExchangeRate eurRate = new ExchangeRate();
        eurRate.setRateDate(eurDate);

        when(exchangeRateRepository.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(eq(Currency.EUR), eq(Currency.HUF)))
                .thenReturn(Optional.of(hufRate));
        when(exchangeRateRepository.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(eq(Currency.EUR), eq(Currency.USD)))
                .thenReturn(Optional.of(usdRate));
        when(exchangeRateRepository.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(eq(Currency.EUR), eq(Currency.EUR)))
                .thenReturn(Optional.of(eurRate));

        ResponseEntity<?> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("HUF", hufDate);
        assertThat(body).containsEntry("USD", usdDate);
        assertThat(body).containsEntry("EUR", eurDate);
    }

    @Test
    void health_shouldHandleMissingRates() {
        ExchangeRate hufRate = new ExchangeRate();
        hufRate.setRateDate(LocalDate.of(2025, 11, 1));

        when(exchangeRateRepository.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(eq(Currency.EUR), eq(Currency.HUF)))
                .thenReturn(Optional.of(hufRate));
        when(exchangeRateRepository.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(eq(Currency.EUR), eq(Currency.USD)))
                .thenReturn(Optional.empty());
        when(exchangeRateRepository.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(eq(Currency.EUR), eq(Currency.EUR)))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.health();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("HUF");
        assertThat(body).doesNotContainKey("USD");
        assertThat(body).doesNotContainKey("EUR");
    }
}
