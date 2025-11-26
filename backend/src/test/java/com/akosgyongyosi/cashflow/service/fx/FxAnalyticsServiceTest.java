package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.FxVolatilityDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class FxAnalyticsServiceTest {

    private ExchangeRateRepository repo;
    private FxProperties props;
    private FxAnalyticsService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(ExchangeRateRepository.class);
        props = new FxProperties();
        props.setCanonicalBase(Currency.EUR);
        props.setQuotes(EnumSet.of(Currency.HUF, Currency.USD));
        service = new FxAnalyticsService(repo, props);
    }

    private ExchangeRate er(LocalDate date, Currency base, Currency quote, double mid) {
        ExchangeRate e = new ExchangeRate();
        e.setRateDate(date);
        e.setBaseCurrency(base);
        e.setQuoteCurrency(quote);
        e.setRateMid(BigDecimal.valueOf(mid));
        e.setProvider("TEST");
        e.setFetchedAt(Instant.now());
        return e;
    }

    @Test
    void emptyWindowYieldsPartialWithNulls() {
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusDays(29); // window 30
        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(Currency.EUR, Currency.HUF, start, end))
                .thenReturn(List.of());
        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(Currency.EUR, Currency.USD, start, end))
                .thenReturn(List.of());
        List<FxVolatilityDTO> out = service.getVolatility(30);
    assertThat(out).hasSize(2).allMatch(dto -> dto.isPartial() && dto.getSampleSize() == 0);
    }

    @Test
    void computesStatsForSample() {
        // 5 days sample for HUF: rates 400, 402, 401, 403, 404
        LocalDate end = LocalDate.now().minusDays(1);
        List<ExchangeRate> hufRates = List.of(
                er(end.minusDays(4), Currency.EUR, Currency.HUF, 400),
                er(end.minusDays(3), Currency.EUR, Currency.HUF, 402),
                er(end.minusDays(2), Currency.EUR, Currency.HUF, 401),
                er(end.minusDays(1), Currency.EUR, Currency.HUF, 403),
                er(end, Currency.EUR, Currency.HUF, 404)
        );
        LocalDate start = end.minusDays(29); // requested window 30 -> partial
        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(Currency.EUR, Currency.HUF, start, end))
                .thenReturn(hufRates);
        when(repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(Currency.EUR, Currency.USD, start, end))
                .thenReturn(List.of());

        List<FxVolatilityDTO> out = service.getVolatility(30);
        FxVolatilityDTO hufDto = out.stream().filter(d -> d.getQuote() == Currency.HUF).findFirst().orElseThrow();
        assertThat(hufDto.getSampleSize()).isEqualTo(5);
        assertThat(hufDto.isPartial()).isTrue();
        // mean
        assertThat(hufDto.getMean()).isEqualByComparingTo("402.0000000000");
        // min/max
        assertThat(hufDto.getMin()).isEqualByComparingTo("400");
        assertThat(hufDto.getMax()).isEqualByComparingTo("404");
        // std dev (sample): values diff from mean: -2,0,-1,1,2 -> squares:4,0,1,1,4 sum=10 variance=10/(5-1)=2.5 sqrt=1.5811388301
        assertThat(hufDto.getStdDev()).isEqualByComparingTo("1.5811388301");
    }
}
