package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.dto.FxWarningCode;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.config.FxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLookupServiceTest {

    private ExchangeRateRepository repo;
    private FxRateEnsurer ensurer;
        private RateLookupService service;
        private ExchangeRateCache fxCache;
        private FxProperties props;
    private FxLookupAuditService auditService;

    @BeforeEach
    void setUp() {
                repo = Mockito.mock(ExchangeRateRepository.class);
                ensurer = Mockito.mock(FxRateEnsurer.class);
                auditService = Mockito.mock(FxLookupAuditService.class);
                props = new FxProperties(); props.setDynamicFetchEnabled(true); // tests rely on dynamic path behavior
                fxCache = Mockito.mock(ExchangeRateCache.class);
                service = new RateLookupService(repo, ensurer, auditService, fxCache, props);
    }

    private ExchangeRate makeRate(LocalDate date, Currency base, Currency quote, double mid) {
        ExchangeRate er = new ExchangeRate();
        er.setRateDate(date);
        er.setBaseCurrency(base);
        er.setQuoteCurrency(quote);
        er.setRateMid(BigDecimal.valueOf(mid));
        er.setProvider("TEST");
        er.setFetchedAt(Instant.now());
        return er;
    }

    @Test
    void futureDateTriggersProvisionalWarnings() {
        LocalDate future = LocalDate.now().plusDays(7);
        LocalDate today = LocalDate.now();
        ExchangeRate todayRate = makeRate(today, Currency.EUR, Currency.HUF, 400.0);
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(today, Currency.EUR, Currency.HUF))
                .thenReturn(Optional.of(todayRate));

        var result = service.lookup(Currency.EUR, Currency.HUF, future);
        assertThat(result.getRate()).isEqualByComparingTo("400.0");
        assertThat(result.isProvisional()).isTrue();
        assertThat(result.getWarnings()).extracting(w -> w.getCode())
                .contains(FxWarningCode.FUTURE_DATE_FALLBACK, FxWarningCode.PROVISIONAL_RATE);
        verify(ensurer).ensureFor(today);
    }

    @Test
    void exactDateNoWarnings() {
        LocalDate day = LocalDate.now().minusDays(3);
        ExchangeRate rate = makeRate(day, Currency.EUR, Currency.USD, 1.08);
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(day, Currency.EUR, Currency.USD))
                .thenReturn(Optional.of(rate));
        var result = service.lookup(Currency.EUR, Currency.USD, day);
        assertThat(result.getWarnings()).isEmpty();
        assertThat(result.getRate()).isEqualByComparingTo("1.08");
        verify(ensurer).ensureFor(day);
    }

    @Test
    void fallbackToEarlierDateEmitsWarning() {
        LocalDate requested = LocalDate.now().minusDays(10);
        LocalDate earlier = requested.minusDays(2);
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(requested, Currency.EUR, Currency.HUF))
                .thenReturn(Optional.empty());
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(Currency.EUR, Currency.HUF, requested))
                .thenReturn(Optional.of(makeRate(earlier, Currency.EUR, Currency.HUF, 395.5)));
        var result = service.lookup(Currency.EUR, Currency.HUF, requested);
        assertThat(result.getWarnings()).extracting(w -> w.getCode())
                .contains(FxWarningCode.HISTORICAL_GAP_FALLBACK);
        assertThat(result.getRate()).isEqualByComparingTo("395.5");
    }

    @Test
    void cachingPreventsSecondRepoHit() {
        LocalDate day = LocalDate.now().minusDays(5);
        ExchangeRate rate = makeRate(day, Currency.EUR, Currency.HUF, 402.25);
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(day, Currency.EUR, Currency.HUF))
                .thenReturn(Optional.of(rate));
        var first = service.lookup(Currency.EUR, Currency.HUF, day);
        var second = service.lookup(Currency.EUR, Currency.HUF, day);
        assertThat(second.getRate()).isEqualByComparingTo(first.getRate());
        verify(repo, times(1)).findByRateDateAndBaseCurrencyAndQuoteCurrency(day, Currency.EUR, Currency.HUF);
    }
}
