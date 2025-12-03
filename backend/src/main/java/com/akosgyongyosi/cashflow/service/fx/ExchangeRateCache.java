package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.config.FxProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class ExchangeRateCache {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateCache.class);

    private final ExchangeRateRepository repo;
    private final FxProperties props;

    private final Map<Currency, NavigableMap<LocalDate, BigDecimal>> rates = new ConcurrentHashMap<>();

    public ExchangeRateCache(ExchangeRateRepository repo, FxProperties props) {
        this.repo = repo; this.props = props;
    }

    @PostConstruct
    public void init() {
    }

    public void loadAll(int days) {
        rates.clear();
        LocalDate today = LocalDate.now();
    LocalDate start = today.minusDays(days - 1L);
        Currency base = props.getCanonicalBase();
        Set<Currency> quotes = new HashSet<>(props.getQuotes());
        quotes.remove(base);
        for (Currency q : quotes) {
            NavigableMap<LocalDate, BigDecimal> series = new TreeMap<>();
            List<ExchangeRate> rows = repo.findByBaseCurrencyAndQuoteCurrencyAndRateDateBetweenOrderByRateDateAsc(base, q, start, today);
            for (ExchangeRate er : rows) {
                series.put(er.getRateDate(), er.getRateMid());
            }
            rates.put(q, series);
            log.info("FX cache loaded base={} quote={} daysLoaded={} missingDays={}", base, q, series.size(), days - series.size());
        }
    }

   
    public BigDecimal getRate(LocalDate date, Currency quote) {
        NavigableMap<LocalDate, BigDecimal> series = rates.get(quote);
        if (series == null || series.isEmpty()) {
            throw new IllegalStateException("Rate series empty for quote=" + quote);
        }
        LocalDate today = LocalDate.now();
        if (date.isAfter(today)) {
            return series.lastEntry().getValue();
        }
        BigDecimal exact = series.get(date);
        if (exact != null) return exact;
        Map.Entry<LocalDate, BigDecimal> prior = series.floorEntry(date);
        Map.Entry<LocalDate, BigDecimal> next  = series.ceilingEntry(date);
        if (prior != null && next != null) {
            return prior.getValue();
        }
        if (prior != null) return prior.getValue();
        if (next != null) return next.getValue();
        return series.firstEntry().getValue();
    }

    public LocalDate earliestDate(Currency quote) {
        NavigableMap<LocalDate, BigDecimal> series = rates.get(quote);
        return (series == null || series.isEmpty()) ? null : series.firstKey();
    }
    public LocalDate latestDate(Currency quote) {
        NavigableMap<LocalDate, BigDecimal> series = rates.get(quote);
        return (series == null || series.isEmpty()) ? null : series.lastKey();
    }
}
