package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FxServiceTest {

    private ExchangeRateRepository repo;
    private FxService service;

    @BeforeEach
    void setUp() {
        repo = mock(ExchangeRateRepository.class);
        service = new FxService(repo);
    }

    @Test
    void convert_same_currency_returns_amount_unchanged() {
        BigDecimal amount = BigDecimal.valueOf(100);
        LocalDate date = LocalDate.of(2024, 12, 1);

        BigDecimal result = service.convert(amount, Currency.HUF, Currency.HUF, date);

        assertThat(result).isEqualByComparingTo(amount);
        verifyNoInteractions(repo);
    }

    @Test
    void convert_from_eur_to_huf_uses_direct_rate() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(Currency.EUR);
        rate.setQuoteCurrency(Currency.HUF);
        rate.setRateMid(BigDecimal.valueOf(400));
        rate.setRateDate(date);

        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                Currency.EUR, Currency.HUF, date))
                .thenReturn(Optional.of(rate));

        BigDecimal result = service.convert(BigDecimal.valueOf(10), Currency.EUR, Currency.HUF, date);

        // 10 EUR * 400 / 1 = 4000 HUF
        assertThat(result).isEqualByComparingTo("4000");
    }

    @Test
    void convert_from_huf_to_eur_uses_inverse_rate() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        ExchangeRate rateHuf = new ExchangeRate();
        rateHuf.setBaseCurrency(Currency.EUR);
        rateHuf.setQuoteCurrency(Currency.HUF);
        rateHuf.setRateMid(BigDecimal.valueOf(400));
        rateHuf.setRateDate(date);

        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                Currency.EUR, Currency.HUF, date))
                .thenReturn(Optional.of(rateHuf));
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                Currency.EUR, Currency.EUR, date))
                .thenReturn(Optional.empty());

        BigDecimal result = service.convert(BigDecimal.valueOf(4000), Currency.HUF, Currency.EUR, date);

        // 4000 HUF * 1 / 400 = 10 EUR
        assertThat(result).isEqualByComparingTo("10");
    }

    @Test
    void convert_cross_rate_huf_to_usd_uses_both_rates() {
        LocalDate date = LocalDate.of(2024, 12, 1);
        ExchangeRate rateHuf = new ExchangeRate();
        rateHuf.setRateMid(BigDecimal.valueOf(400));
        ExchangeRate rateUsd = new ExchangeRate();
        rateUsd.setRateMid(BigDecimal.valueOf(1.1));

        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                Currency.EUR, Currency.USD, date))
                .thenReturn(Optional.of(rateUsd));
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                Currency.EUR, Currency.HUF, date))
                .thenReturn(Optional.of(rateHuf));

        BigDecimal result = service.convert(BigDecimal.valueOf(400), Currency.HUF, Currency.USD, date);

        // 400 HUF * 1.1 / 400 = 1.1 USD
        assertThat(result).isEqualByComparingTo("1.1");
    }

    @Test
    void convert_throws_when_no_rate_available() {
        LocalDate date = LocalDate.of(2020, 1, 1);
        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                any(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(BigDecimal.TEN, Currency.HUF, Currency.EUR, date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No FX rate");
    }

    @Test
    void convert_uses_most_recent_rate_on_or_before_date() {
        LocalDate date = LocalDate.of(2024, 12, 15);
        ExchangeRate oldRate = new ExchangeRate();
        oldRate.setRateMid(BigDecimal.valueOf(390));
        oldRate.setRateDate(LocalDate.of(2024, 12, 10));

        when(repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                Currency.EUR, Currency.HUF, date))
                .thenReturn(Optional.of(oldRate));

        BigDecimal result = service.convert(BigDecimal.TEN, Currency.EUR, Currency.HUF, date);

        assertThat(result).isEqualByComparingTo("3900");
    }
}
