package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.RateLookupResultDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Diagnostic tests for RateLookupService focusing on provisional future date handling and EUR->HUF availability.
 */
class RateLookupServiceProvisionalFutureTest {

    private ExchangeRateRepository repo;
    private FxRateEnsurer ensurer;
    private FxLookupAuditService auditService;
    private ExchangeRateCache cache;
    private FxProperties props;
    private RateLookupService lookup;

    @BeforeEach
    void setUp() {
        repo = mock(ExchangeRateRepository.class);
        ensurer = mock(FxRateEnsurer.class);
        auditService = mock(FxLookupAuditService.class);
        cache = mock(ExchangeRateCache.class);
        props = new FxProperties();
        props.setDynamicFetchEnabled(true);
        lookup = new RateLookupService(repo, ensurer, auditService, cache, props);
    }

    @Test
    void futureDateLookup_marksProvisional_andAddsWarnings() {
        LocalDate futureBooking = LocalDate.now().plusMonths(2); // simulate future
        LocalDate today = LocalDate.now();

        // Provide today's rate for EUR->HUF
        ExchangeRate eurHufToday = new ExchangeRate();
        eurHufToday.setBaseCurrency(Currency.EUR);
        eurHufToday.setQuoteCurrency(Currency.HUF);
        eurHufToday.setRateDate(today);
        eurHufToday.setRateMid(BigDecimal.valueOf(400));
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(today, Currency.EUR, Currency.HUF))
                .thenReturn(Optional.of(eurHufToday));

        RateLookupResultDTO result = lookup.lookup(Currency.EUR, Currency.HUF, futureBooking);

        assertThat(result.isProvisional()).isTrue();
        assertThat(result.getRate()).isEqualByComparingTo("400");
        assertThat(result.getWarnings()).isNotEmpty();
        assertThat(result.getWarnings().stream().map(w -> w.getCode().name()).toList())
                .contains("FUTURE_DATE_FALLBACK", "PROVISIONAL_RATE");
    }

    @Test
    void historicalDateLookup_returnsExactOrFallbackRate_withoutProvisionalFlag() {
        LocalDate date = LocalDate.of(2025, 1, 15);
        ExchangeRate eurHuf = new ExchangeRate();
        eurHuf.setBaseCurrency(Currency.EUR);
        eurHuf.setQuoteCurrency(Currency.HUF);
        eurHuf.setRateDate(date);
        eurHuf.setRateMid(BigDecimal.valueOf(410.25));
        when(repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(date, Currency.EUR, Currency.HUF))
                .thenReturn(Optional.of(eurHuf));

        RateLookupResultDTO result = lookup.lookup(Currency.EUR, Currency.HUF, date);

        assertThat(result.isProvisional()).isFalse();
        assertThat(result.getRate()).isEqualByComparingTo("410.25");
        assertThat(result.getWarnings()).isEmpty();
    }
}
