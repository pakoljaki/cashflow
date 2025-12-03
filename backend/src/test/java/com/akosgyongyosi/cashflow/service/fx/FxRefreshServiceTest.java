package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FxRefreshServiceTest {

    @Mock
    private FxIngestionService ingestionService;
    
    @Mock
    private ExchangeRateCache cache;
    
    @Mock
    private ExchangeRateRepository exchangeRateRepository;
    
    private FxProperties props;
    private FxRefreshService refreshService;

    @BeforeEach
    void setup() {
        props = new FxProperties();
        props.setEnabled(true);
        props.setCanonicalBase(Currency.EUR);
        props.setQuotes(EnumSet.of(Currency.HUF, Currency.USD));
        props.setStartupBackfillDays(30);
        props.setChunkSizeDays(10);
        
        refreshService = new FxRefreshService(ingestionService, props, cache, exchangeRateRepository);
    }

    @Test
    void refreshExchangeRates_allRatesPresent_noIngestion() {
        // Given: All rates are present in DB
        when(exchangeRateRepository.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.of(new ExchangeRate()));
        
        // When
        int result = refreshService.refreshExchangeRates(30);
        
        // Then: No ingestion windows triggered
        assertEquals(0, result);
        verify(ingestionService, never()).fetchAndUpsert(any(LocalDate.class));
        verify(ingestionService, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
        verify(cache).loadAll(30);
    }

    @Test
    void refreshExchangeRates_missingRates_triggersIngestion() {
        // Given: Some rates are missing
        LocalDate today = LocalDate.now();
        when(exchangeRateRepository.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty()); // Simulate missing rates
        
        // When
        int result = refreshService.refreshExchangeRates(30);
        
        // Then: Ingestion windows were triggered
        verify(ingestionService, atLeastOnce()).fetchAndUpsert(any(LocalDate.class));
        verify(ingestionService, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
        verify(cache).loadAll(30);
        assertTrue(result > 0);
    }

    @Test
    void refreshExchangeRates_ingestionFailure_continuesWithOtherWindows() {
        // Given: Ingestion will fail for first window
        when(exchangeRateRepository.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(ingestionService.fetchAndUpsert(any(LocalDate.class)))
            .thenThrow(new RuntimeException("API error"))
            .thenReturn(null);
        
        // When
        int result = refreshService.refreshExchangeRates(30);
        
        // Then: Should continue despite failure
        verify(cache).loadAll(30);
        assertTrue(result >= 0);
    }

    @Test
    void refreshExchangeRates_largeBackfill_fetchesEachMissingDay() {
        // Given: Large backfill with some missing rates
        when(exchangeRateRepository.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(ingestionService.fetchAndUpsert(any(LocalDate.class))).thenReturn(null);
        
        // When: Request 100 days with chunk size 10
        int days = 100;
        props.setChunkSizeDays(10);
        int expectedMissingDays = days - 1; // yesterday through start inclusive
        refreshService.refreshExchangeRates(days);

        // Then: Each missing day should trigger a fetch
        verify(ingestionService, times(expectedMissingDays)).fetchAndUpsert(any(LocalDate.class));
        verify(ingestionService, never()).fetchAndUpsert(any(LocalDate.class), any(LocalDate.class));
        verify(cache).loadAll(days);
    }
}
