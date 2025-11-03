package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FxRequestCacheTest {

    private FxService fxService;
    private FxRequestCache cache;

    @BeforeEach
    void setUp() {
        fxService = mock(FxService.class);
        cache = new FxRequestCache(fxService);
    }

    @Test
    void convert_same_currency_returns_unchanged() {
        BigDecimal amount = BigDecimal.valueOf(100);
        LocalDate date = LocalDate.of(2024, 12, 1);

        BigDecimal result = cache.convert(amount, Currency.HUF, Currency.HUF, date);

        assertThat(result).isEqualByComparingTo(amount);
        verifyNoInteractions(fxService);
    }

    @Test
    void convert_calls_fxService_once_and_caches_rate() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date))
                .thenReturn(BigDecimal.valueOf(400));

        BigDecimal result1 = cache.convert(BigDecimal.valueOf(10), Currency.EUR, Currency.HUF, date);
        BigDecimal result2 = cache.convert(BigDecimal.valueOf(20), Currency.EUR, Currency.HUF, date);

        assertThat(result1).isEqualByComparingTo("4000"); // 10 * 400
        assertThat(result2).isEqualByComparingTo("8000"); // 20 * 400
        verify(fxService, times(1)).convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date);
    }

    @Test
    void convert_different_date_triggers_separate_cache_entry() {
        LocalDate date1 = LocalDate.of(2024, 12, 1);
        LocalDate date2 = LocalDate.of(2024, 12, 15);
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date1))
                .thenReturn(BigDecimal.valueOf(390));
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date2))
                .thenReturn(BigDecimal.valueOf(400));

        BigDecimal result1 = cache.convert(BigDecimal.TEN, Currency.EUR, Currency.HUF, date1);
        BigDecimal result2 = cache.convert(BigDecimal.TEN, Currency.EUR, Currency.HUF, date2);

        assertThat(result1).isEqualByComparingTo("3900");
        assertThat(result2).isEqualByComparingTo("4000");
        verify(fxService, times(1)).convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date1);
        verify(fxService, times(1)).convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date2);
    }

    @Test
    void convert_different_currency_pair_triggers_separate_cache_entry() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date))
                .thenReturn(BigDecimal.valueOf(400));
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.USD, date))
                .thenReturn(BigDecimal.valueOf(1.1));

        cache.convert(BigDecimal.TEN, Currency.EUR, Currency.HUF, date);
        cache.convert(BigDecimal.TEN, Currency.EUR, Currency.USD, date);

        verify(fxService).convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, date);
        verify(fxService).convert(BigDecimal.ONE, Currency.EUR, Currency.USD, date);
    }
}
