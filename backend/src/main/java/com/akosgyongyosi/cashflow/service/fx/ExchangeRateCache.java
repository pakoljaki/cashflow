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

/**
 * In-memory cache of historical FX rates (base->quote) preloaded at startup.
 * After refactor we avoid any external provider calls during normal request flow.
 */
@Component
public class ExchangeRateCache {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateCache.class);

    private final ExchangeRateRepository repo;
    private final FxProperties props;

    // Map: quote currency -> NavigableMap<date, rate>
    private final Map<Currency, NavigableMap<LocalDate, BigDecimal>> rates = new ConcurrentHashMap<>();

    public ExchangeRateCache(ExchangeRateRepository repo, FxProperties props) {
        this.repo = repo; this.props = props;
    }

    @PostConstruct
    public void init() {
        // Lazy initialization; actual loading triggered by StartupFullRangeIngestionRunner
    }

    /** Load or reload cache for past N days ending today inclusive. */
    public void loadAll(int days) {
        rates.clear();
        LocalDate today = LocalDate.now();
    LocalDate start = today.minusDays(days - 1L); // inclusive window
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

    /** Lookup base->quote rate for date with cache-only fallback rules:
     *  1. Future date (> today): latest available (treat as provisional by caller).
     *  2. Exact historical date present: return it.
     *  3. Missing historical date:
     *     - If both prior and next dates exist: use PRIOR (stable, conservative choice).
     *     - If only prior exists: use prior.
     *     - If only next exists (date precedes earliest ingested): use next.
     *  4. Series empty => error (should not happen after startup ingestion).
     */
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
            // Both sides exist -> choose prior per requirement.
            return prior.getValue();
        }
        if (prior != null) return prior.getValue();
        if (next != null) return next.getValue();
        // Should not reach here (series not empty) but defensive fallback.
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
