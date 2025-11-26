package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateCacheTest {
    private ExchangeRateRepository repo;
    private FxProperties props;
    private ExchangeRateCache cache;

    private LocalDate today;
    private LocalDate twoDaysAgo;
    private LocalDate fiveDaysAgo;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        twoDaysAgo = today.minusDays(2);
        fiveDaysAgo = today.minusDays(5);
        repo = mock(ExchangeRateRepository.class);
        props = new FxProperties();
        props.setCanonicalBase(Currency.EUR);
        props.setQuotes(Set.of(Currency.HUF));
        cache = new ExchangeRateCache(repo, props);

        ExchangeRate r5 = new ExchangeRate();
        r5.setRateDate(fiveDaysAgo); r5.setBaseCurrency(Currency.EUR); r5.setQuoteCurrency(Currency.HUF); r5.setRateMid(new BigDecimal("385.00"));
        ExchangeRate r2 = new ExchangeRate();
        r2.setRateDate(twoDaysAgo); r2.setBaseCurrency(Currency.EUR); r2.setQuoteCurrency(Currency.HUF); r2.setRateMid(new BigDecimal("388.00"));
        ExchangeRate r0 = new ExchangeRate();
        r0.setRateDate(today); r0.setBaseCurrency(Currency.EUR); r0.setQuoteCurrency(Currency.HUF); r0.setRateMid(new BigDecimal("389.50"));

        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(
                eq(Currency.EUR), eq(Currency.HUF), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(r5, r2, r0));

        cache.loadAll(7); // load past week
    }

    @Test
    void exactDateReturnsExactRate() {
        assertEquals(new BigDecimal("388.00"), cache.getRate(twoDaysAgo, Currency.HUF));
    }

    @Test
    void missingDateUsesPrior() {
        LocalDate missing = today.minusDays(1); // not present
        assertEquals(new BigDecimal("388.00"), cache.getRate(missing, Currency.HUF), "Should fallback to prior twoDaysAgo rate");
    }

    @Test
    void noPriorUsesEarliest() {
        LocalDate veryOld = today.minusDays(30);
        assertEquals(new BigDecimal("385.00"), cache.getRate(veryOld, Currency.HUF), "Should fallback to earliest available rate");
    }

    @Test
    void futureUsesLatest() {
        LocalDate future = today.plusDays(10);
        assertEquals(new BigDecimal("389.50"), cache.getRate(future, Currency.HUF));
    }
}
