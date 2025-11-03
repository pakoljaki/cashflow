package com.akosgyongyosi.cashflow.controller.admin;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.service.fx.FxIngestionService;
import com.akosgyongyosi.cashflow.service.fx.FxIngestionService.IngestionRangeSummary;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxAdminControllerTest {

    @Mock
    private FxIngestionService ingestionService;

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    private FxAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new FxAdminController(ingestionService, exchangeRateRepository);
    }

    @Test
    void refresh_shouldReturnIngestionSummary() {
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 3);
        
        IngestionRangeSummary summary = new IngestionRangeSummary(start, end);
        summary.add(new FxIngestionService.IngestionSummary(start, 5, 3, Currency.EUR, 2));
        when(ingestionService.fetchAndUpsert(start, end)).thenReturn(summary);

        ResponseEntity<?> response = controller.refresh(start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("start", start);
        assertThat(body).containsEntry("end", end);
        assertThat(body).containsEntry("inserted", 5);
        assertThat(body).containsEntry("updated", 3);
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

    @Test
    void refresh_shouldHandleEmptyDateRange() {
        LocalDate start = LocalDate.of(2025, 11, 1);
        LocalDate end = LocalDate.of(2025, 11, 1);
        
        IngestionRangeSummary summary = new IngestionRangeSummary(start, end);
        when(ingestionService.fetchAndUpsert(start, end)).thenReturn(summary);

        ResponseEntity<?> response = controller.refresh(start, end);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("inserted", 0);
        assertThat(body).containsEntry("updated", 0);
    }
}
