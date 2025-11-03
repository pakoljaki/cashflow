package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.service.fx.provider.FxProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FxIngestionServiceTest {

    @Mock
    private FxProvider provider;

    @Mock
    private ExchangeRateRepository repo;

    @Mock
    private FxProperties props;

    @InjectMocks
    private FxIngestionService fxIngestionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void fetchAndUpsert_shouldInsertNewRates() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR, Currency.HUF);
        
        Map<Currency, BigDecimal> rates = Map.of(
            Currency.EUR, new BigDecimal("0.85"),
            Currency.HUF, new BigDecimal("375.0")
        );

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getDailyQuotes(date, base, quotes)).thenReturn(rates);
        when(provider.getProviderName()).thenReturn("TestProvider");
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionSummary summary = fxIngestionService.fetchAndUpsert(date);

        assertThat(summary.getDate()).isEqualTo(date);
        assertThat(summary.getInserted()).isEqualTo(2);
        assertThat(summary.getUpdated()).isEqualTo(0);
        assertThat(summary.getBase()).isEqualTo(base);
        assertThat(summary.getRequestedQuotes()).isEqualTo(2);
        
        verify(repo, times(2)).save(any(ExchangeRate.class));
        verify(provider).getDailyQuotes(date, base, quotes);
    }

    @Test
    void fetchAndUpsert_shouldUpdateExistingRates() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR);
        
        ExchangeRate existingRate = new ExchangeRate();
        existingRate.setId(1L);
        existingRate.setRateDate(date);
        existingRate.setBaseCurrency(base);
        existingRate.setQuoteCurrency(Currency.EUR);
        existingRate.setRateMid(new BigDecimal("0.85"));
        
        Map<Currency, BigDecimal> rates = Map.of(Currency.EUR, new BigDecimal("0.87"));

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getDailyQuotes(date, base, quotes)).thenReturn(rates);
        when(provider.getProviderName()).thenReturn("TestProvider");
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(date, base, Currency.EUR))
            .thenReturn(Optional.of(existingRate));
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionSummary summary = fxIngestionService.fetchAndUpsert(date);

        assertThat(summary.getInserted()).isEqualTo(0);
        assertThat(summary.getUpdated()).isEqualTo(1);
        
        ArgumentCaptor<ExchangeRate> captor = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRateMid()).isEqualByComparingTo("0.87");
    }

    @Test
    void fetchAndUpsert_shouldSkipBaseToBaseCurrency() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.USD, Currency.EUR);
        
        Map<Currency, BigDecimal> rates = Map.of(
            Currency.USD, BigDecimal.ONE,
            Currency.EUR, new BigDecimal("0.85")
        );

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getDailyQuotes(date, base, quotes)).thenReturn(rates);
        when(provider.getProviderName()).thenReturn("TestProvider");
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionSummary summary = fxIngestionService.fetchAndUpsert(date);

        assertThat(summary.getInserted()).isEqualTo(1); // Only EUR, not USD->USD
        verify(repo, times(1)).save(any(ExchangeRate.class));
    }

    @Test
    void fetchAndUpsert_shouldSkipNullRates() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR, Currency.HUF);
        
        Map<Currency, BigDecimal> rates = Map.of(Currency.EUR, new BigDecimal("0.85"));
        // HUF rate is null/missing

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getDailyQuotes(date, base, quotes)).thenReturn(rates);
        when(provider.getProviderName()).thenReturn("TestProvider");
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionSummary summary = fxIngestionService.fetchAndUpsert(date);

        assertThat(summary.getInserted()).isEqualTo(1); // Only EUR
        verify(repo, times(1)).save(any(ExchangeRate.class));
    }

    @Test
    void fetchAndUpsert_shouldThrowExceptionOnProviderFailure() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR);

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getDailyQuotes(date, base, quotes))
            .thenThrow(new RuntimeException("Provider API error"));

        assertThatThrownBy(() -> fxIngestionService.fetchAndUpsert(date))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Provider API error");
        
        verify(repo, never()).save(any());
    }

    @Test
    void fetchAndUpsertRange_shouldIngestMultipleDates() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 3);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR);

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getProviderName()).thenReturn("TestProvider");
        
        when(provider.getDailyQuotes(any(), eq(base), eq(quotes)))
            .thenReturn(Map.of(Currency.EUR, new BigDecimal("0.85")));
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionRangeSummary summary = 
            fxIngestionService.fetchAndUpsert(start, end);

        assertThat(summary.getStart()).isEqualTo(start);
        assertThat(summary.getEnd()).isEqualTo(end);
        assertThat(summary.getTotalInserted()).isEqualTo(3); // 3 days
        assertThat(summary.getTotalUpdated()).isEqualTo(0);
        
        verify(provider, times(3)).getDailyQuotes(any(), eq(base), eq(quotes));
        verify(repo, times(3)).save(any(ExchangeRate.class));
    }

    @Test
    void fetchAndUpsertRange_shouldHandleSingleDayRange() {
        LocalDate date = LocalDate.of(2024, 1, 15);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR);

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getProviderName()).thenReturn("TestProvider");
        when(provider.getDailyQuotes(date, base, quotes))
            .thenReturn(Map.of(Currency.EUR, new BigDecimal("0.85")));
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(any(), any(), any()))
            .thenReturn(Optional.empty());
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionRangeSummary summary = 
            fxIngestionService.fetchAndUpsert(date, date);

        assertThat(summary.getTotalInserted()).isEqualTo(1);
        verify(provider, times(1)).getDailyQuotes(date, base, quotes);
    }

    @Test
    void fetchAndUpsertRange_shouldCombineInsertAndUpdateCounts() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 2);
        Currency base = Currency.USD;
        Set<Currency> quotes = Set.of(Currency.EUR);

        ExchangeRate existingRate = new ExchangeRate();
        existingRate.setId(1L);

        when(props.getCanonicalBase()).thenReturn(base);
        when(props.getQuotes()).thenReturn(quotes);
        when(provider.getProviderName()).thenReturn("TestProvider");
        when(provider.getDailyQuotes(any(), eq(base), eq(quotes)))
            .thenReturn(Map.of(Currency.EUR, new BigDecimal("0.85")));
        
        // First date: insert, Second date: update
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(start, base, Currency.EUR))
            .thenReturn(Optional.empty());
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(end, base, Currency.EUR))
            .thenReturn(Optional.of(existingRate));
        when(repo.save(any(ExchangeRate.class))).thenAnswer(i -> i.getArgument(0));

        FxIngestionService.IngestionRangeSummary summary = 
            fxIngestionService.fetchAndUpsert(start, end);

        assertThat(summary.getTotalInserted()).isEqualTo(1);
        assertThat(summary.getTotalUpdated()).isEqualTo(1);
    }
}
