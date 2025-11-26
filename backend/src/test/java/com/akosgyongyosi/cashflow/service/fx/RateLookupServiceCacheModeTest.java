package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.RateLookupResultDTO;
import com.akosgyongyosi.cashflow.dto.RateMetaDTO;
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
class RateLookupServiceCacheModeTest {

    private ExchangeRateRepository repo;
    private FxRateEnsurer ensurer; // not used when dynamic fetch disabled
    private FxLookupAuditService auditService; // will be mocked
    private FxProperties props;
    private ExchangeRateCache fxCache;
    private RateLookupService service;

    private LocalDate today;
    private LocalDate twoDaysAgo;
    private LocalDate missingDay; // today-1 (will be absent to test fallback)

    @BeforeEach
    void setup() {
        today = LocalDate.now();
        missingDay = today.minusDays(1); // intentionally absent
        twoDaysAgo = today.minusDays(2);

        repo = mock(ExchangeRateRepository.class);
        ensurer = mock(FxRateEnsurer.class);
        auditService = mock(FxLookupAuditService.class);

        props = new FxProperties();
        props.setCanonicalBase(Currency.EUR);
        props.setQuotes(Set.of(Currency.HUF, Currency.USD));
        props.setDynamicFetchEnabled(false); // critical for cache-only mode

        // Build repository responses for cache loading
        ExchangeRate erToday = new ExchangeRate();
        erToday.setRateDate(today);
        erToday.setBaseCurrency(Currency.EUR);
        erToday.setQuoteCurrency(Currency.HUF);
        erToday.setRateMid(new BigDecimal("390.00"));
        ExchangeRate erTwoDaysAgo = new ExchangeRate();
        erTwoDaysAgo.setRateDate(twoDaysAgo);
        erTwoDaysAgo.setBaseCurrency(Currency.EUR);
        erTwoDaysAgo.setQuoteCurrency(Currency.HUF);
        erTwoDaysAgo.setRateMid(new BigDecimal("388.50"));

        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(
                eq(Currency.EUR), eq(Currency.HUF), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(erTwoDaysAgo, erToday));
        // USD minimal stub (empty list OK)
        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(
                eq(Currency.EUR), eq(Currency.USD), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        fxCache = new ExchangeRateCache(repo, props);
        fxCache.loadAll(3); // load 3-day window including today and twoDaysAgo

        service = new RateLookupService(repo, ensurer, auditService, fxCache, props);
    }

    @Test
    void exactHistoricalDateReturnsExactRate() {
        RateLookupResultDTO result = service.lookup(Currency.EUR, Currency.HUF, twoDaysAgo);
        assertNotNull(result);
        assertEquals(new BigDecimal("388.50"), result.getRate(), "Exact date rate mismatch");
        RateMetaDTO meta = result.getMeta();
    assertEquals(twoDaysAgo, meta.getRateDateUsed(), "Meta date should be the requested historical date");
        assertFalse(meta.isProvisional(), "Historical date should not be provisional");
    }

    @Test
    void missingDateFallsBackToPriorDateRate() {
        RateLookupResultDTO result = service.lookup(Currency.EUR, Currency.HUF, missingDay);
        assertNotNull(result);
        assertEquals(new BigDecimal("388.50"), result.getRate(), "Fallback prior date rate mismatch");
    assertEquals(missingDay.isAfter(LocalDate.now()) ? LocalDate.now() : missingDay, result.getMeta().getRateDateUsed());
        assertFalse(result.getMeta().isProvisional(), "Past missing date should not be provisional");
    }

    @Test
    void futureDateUsesLatestAndProvisionalFlag() {
        LocalDate future = today.plusDays(10);
        RateLookupResultDTO result = service.lookup(Currency.EUR, Currency.HUF, future);
        assertNotNull(result);
        assertEquals(new BigDecimal("390.00"), result.getRate(), "Future date should use latest available rate");
    assertEquals(today, result.getMeta().getRateDateUsed(), "Meta date for future should collapse to today");
        assertTrue(result.getMeta().isProvisional(), "Future date rate should be provisional");
    }
}
