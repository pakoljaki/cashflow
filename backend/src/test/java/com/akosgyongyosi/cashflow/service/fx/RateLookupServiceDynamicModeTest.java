package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.FxWarningCode;
import com.akosgyongyosi.cashflow.dto.RateLookupResultDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLookupServiceDynamicModeTest {
    private ExchangeRateRepository repo;
    private FxRateEnsurer ensurer;
    private FxLookupAuditService audit;
    private ExchangeRateCache fxCache; // not used in dynamic path tests
    private FxProperties props;
    private RateLookupService service;

    private LocalDate today;
    private LocalDate past;
    private LocalDate missingPast;

    @BeforeEach
    void setup() {
        repo = mock(ExchangeRateRepository.class);
        ensurer = mock(FxRateEnsurer.class);
        audit = mock(FxLookupAuditService.class);
        fxCache = mock(ExchangeRateCache.class);
        props = new FxProperties();
        props.setDynamicFetchEnabled(true); // exercise dynamic path
        props.setCanonicalBase(Currency.EUR);
        today = LocalDate.now();
        past = today.minusDays(10);
        missingPast = today.minusDays(5); // will be absent to force fallbacks
        service = new RateLookupService(repo, ensurer, audit, fxCache, props);
    }

    private ExchangeRate er(LocalDate d, BigDecimal mid) {
        ExchangeRate e = new ExchangeRate();
        e.setRateDate(d); e.setBaseCurrency(Currency.EUR); e.setQuoteCurrency(Currency.USD); e.setRateMid(mid);
        return e;
    }

    @Test
    void exactRateFetchedDynamicPath() {
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(past, Currency.EUR, Currency.USD)).thenReturn(Optional.of(er(past, new BigDecimal("1.10"))));
        RateLookupResultDTO res = service.lookup(Currency.EUR, Currency.USD, past);
        assertEquals(new BigDecimal("1.10"), res.getRate());
        verify(ensurer).ensureFor(past);
    }

    @Test
    void missingHistoricalFallsBackToPrior() {
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(missingPast, Currency.EUR, Currency.USD)).thenReturn(Optional.empty());
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(Currency.EUR, Currency.USD, missingPast))
                .thenReturn(Optional.of(er(past, new BigDecimal("1.11"))));
        RateLookupResultDTO res = service.lookup(Currency.EUR, Currency.USD, missingPast);
        assertEquals(new BigDecimal("1.11"), res.getRate());
        assertTrue(res.getWarnings().stream().anyMatch(w -> w.getCode() == FxWarningCode.HISTORICAL_GAP_FALLBACK));
        verify(ensurer).ensureFor(missingPast);
    }

    @Test
    void missingHistoricalFallsBackToLatest() {
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(missingPast, Currency.EUR, Currency.USD)).thenReturn(Optional.empty());
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(Currency.EUR, Currency.USD, missingPast))
                .thenReturn(Optional.empty());
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(Currency.EUR, Currency.USD))
                .thenReturn(Optional.of(er(today.minusDays(1), new BigDecimal("1.09"))));
        RateLookupResultDTO res = service.lookup(Currency.EUR, Currency.USD, missingPast);
        assertEquals(new BigDecimal("1.09"), res.getRate());
        assertTrue(res.getWarnings().stream().anyMatch(w -> w.getCode() == FxWarningCode.HISTORICAL_GAP_TODAY_FALLBACK));
        verify(ensurer).ensureFor(missingPast);
    }

    @Test
    void futureDateProducesProvisionalWarnings() {
        LocalDate future = today.plusDays(7);
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(today, Currency.EUR, Currency.USD))
                .thenReturn(Optional.of(er(today, new BigDecimal("1.08"))));
        RateLookupResultDTO res = service.lookup(Currency.EUR, Currency.USD, future);
        assertEquals(new BigDecimal("1.08"), res.getRate());
        assertTrue(res.isProvisional());
        assertTrue(res.getWarnings().stream().anyMatch(w -> w.getCode() == FxWarningCode.FUTURE_DATE_FALLBACK));
        assertTrue(res.getWarnings().stream().anyMatch(w -> w.getCode() == FxWarningCode.PROVISIONAL_RATE));
        verify(ensurer).ensureFor(today); // ensureFor called with collapsed rateDate
    }
}
