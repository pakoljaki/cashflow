package com.akosgyongyosi.cashflow.service.fx.provider;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FrankfurterFxProviderTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private FxProperties fxProperties;

    private FrankfurterFxProvider provider;

    @BeforeEach
    void setUp() {
        lenient().when(fxProperties.getApiBaseUrl()).thenReturn("https://api.frankfurter.app");
        provider = new FrankfurterFxProvider(restTemplate, fxProperties);
    }

    @Test
    void getProviderName_shouldReturnFrankfurter() {
        assertThat(provider.getProviderName()).isEqualTo("Frankfurter");
    }

    @Test
    void getDailyQuotes_shouldReturnEmptyMapWhenQuotesIsNull() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, null);

        assertThat(result).isEmpty();
    }

    @Test
    void getDailyQuotes_shouldReturnEmptyMapWhenQuotesIsEmpty() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.noneOf(Currency.class);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).isEmpty();
    }

    @Test
    void getDailyQuotes_shouldFetchAndParseRatesSuccessfully() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.USD, Currency.HUF);

        Map<String, Object> apiResponse = Map.of(
                "base", "EUR",
                "date", "2025-11-03",
                "rates", Map.of(
                        "USD", 1.1,
                        "HUF", 390.5
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).hasSize(2);
        assertThat(result.get(Currency.USD)).isEqualByComparingTo(new BigDecimal("1.1"));
        assertThat(result.get(Currency.HUF)).isEqualByComparingTo(new BigDecimal("390.5"));
    }

    @Test
    void getDailyQuotes_shouldSkipBaseToBaseCurrency() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.EUR, Currency.USD);

        Map<String, Object> apiResponse = Map.of(
                "base", "EUR",
                "date", "2025-11-03",
                "rates", Map.of("USD", 1.1)
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).hasSize(1);
        assertThat(result).containsOnlyKeys(Currency.USD);
    }

    @Test
    void getDailyQuotes_shouldReturnEmptyMapWhenApiReturnsNull() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.USD);

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).isEmpty();
    }

    @Test
    void getDailyQuotes_shouldHandleMissingRatesField() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.USD);

        Map<String, Object> apiResponse = Map.of(
                "base", "EUR",
                "date", "2025-11-03"
                // No "rates" field
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).isEmpty();
    }

    @Test
    void getDailyQuotes_shouldSkipUnknownCurrencyCodes() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.USD);

        Map<String, Object> apiResponse = Map.of(
                "base", "EUR",
                "date", "2025-11-03",
                "rates", Map.of(
                        "USD", 1.1,
                        "XYZ", 999.0  // Unknown currency code
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).hasSize(1);
        assertThat(result).containsOnlyKeys(Currency.USD);
    }

    @Test
    void getDailyQuotes_shouldSkipNullRateValues() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.USD, Currency.HUF);

        Map<String, Object> apiResponse = Map.of(
                "base", "EUR",
                "date", "2025-11-03",
                "rates", Map.of(
                        "USD", 1.1,
                        "HUF", "invalid"  // Non-numeric value
                )
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).hasSize(1);
        assertThat(result).containsOnlyKeys(Currency.USD);
    }

    @Test
    void getDailyQuotes_shouldHandleIntegerRates() {
        LocalDate date = LocalDate.of(2025, 11, 3);
        Currency base = Currency.EUR;
        Set<Currency> quotes = EnumSet.of(Currency.HUF);

        Map<String, Object> apiResponse = Map.of(
                "base", "EUR",
                "date", "2025-11-03",
                "rates", Map.of("HUF", 390)  // Integer instead of double
        );

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(apiResponse);

        Map<Currency, BigDecimal> result = provider.getDailyQuotes(date, base, quotes);

        assertThat(result).hasSize(1);
        assertThat(result.get(Currency.HUF)).isEqualByComparingTo(new BigDecimal("390"));
    }
}
