package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import com.akosgyongyosi.cashflow.service.fx.provider.FxProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FxIngestionService {

    private static final Logger log = LoggerFactory.getLogger(FxIngestionService.class);

    private final FxProvider provider;
    private final ExchangeRateRepository repo;
    private final FxProperties props;

    private static final AtomicLong INSERTED = new AtomicLong();
    private static final AtomicLong UPDATED  = new AtomicLong();
    private static final AtomicLong FAILURES = new AtomicLong();

    public FxIngestionService(FxProvider provider,
                              ExchangeRateRepository repo,
                              FxProperties props) {
        this.provider = provider;
        this.repo = repo;
        this.props = props;
    }

    @Transactional
    public IngestionSummary fetchAndUpsert(LocalDate date) {
        try {
            Currency base = props.getCanonicalBase();
            Set<Currency> quotes = props.getQuotes();
            Map<Currency, BigDecimal> rates = provider.getDailyQuotes(date, base, quotes);
            int inserted = 0;
            int updated = 0;

            for (Currency q : quotes) {
                if (q == base) continue;
                BigDecimal rate = rates.get(q);
                if (rate == null) continue;

                ExchangeRate er = repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(date, base, q)
                        .orElseGet(ExchangeRate::new);
                boolean isNew = er.getId() == null;

                er.setRateDate(date);
                er.setBaseCurrency(base);
                er.setQuoteCurrency(q);
                er.setRateMid(rate);
                er.setProvider(provider.getProviderName());
                er.setFetchedAt(Instant.now());
                repo.save(er);

                if (isNew) { inserted++; INSERTED.incrementAndGet(); }
                else { updated++; UPDATED.incrementAndGet(); }
            }

            log.info("FX ingest {} base={} quotes={} inserted={} updated={} (cumulative: ins={}, upd={})",
                    date, base, quotes, inserted, updated,
                    INSERTED.get(), UPDATED.get());

            warnIfStale();
            return new IngestionSummary(date, inserted, updated, base, quotes.size());
        } catch (Exception ex) {
            long f = FAILURES.incrementAndGet();
            log.error("FX ingest failed for {} (failures={}): {}", date, f, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Transactional
    public IngestionRangeSummary fetchAndUpsert(LocalDate startInclusive, LocalDate endInclusive) {
        IngestionRangeSummary sum = new IngestionRangeSummary(startInclusive, endInclusive);
        for (LocalDate d = startInclusive; !d.isAfter(endInclusive); d = d.plusDays(1)) {
            sum.add(fetchAndUpsert(d));
        }
        log.info("FX ingest range {}..{} -> inserted={}, updated={}",
                startInclusive, endInclusive, sum.getTotalInserted(), sum.getTotalUpdated());
        return sum;
    }

    private void warnIfStale() {
        int warnDays = props.getStalenessWarnDays();
        Currency base = props.getCanonicalBase();
        for (Currency q : props.getQuotes()) {
            if (q == base) continue;
            repo.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(base, q).ifPresent(er -> {
                long age = ChronoUnit.DAYS.between(er.getRateDate(), LocalDate.now());
                if (age > warnDays) {
                    log.warn("FX WARNING: Latest {}->{} rate is {} days old (date={} provider={})",
                            base, q, age, er.getRateDate(), er.getProvider());
                }
            });
        }
    }

    public static final class IngestionSummary {
        private final LocalDate date; private final int inserted; private final int updated;
        private final Currency base; private final int requestedQuotes;
        public IngestionSummary(LocalDate date, int inserted, int updated, Currency base, int requestedQuotes) {
            this.date = date; this.inserted = inserted; this.updated = updated;
            this.base = base; this.requestedQuotes = requestedQuotes;
        }
        public LocalDate getDate() { return date; }
        public int getInserted() { return inserted; }
        public int getUpdated() { return updated; }
        public Currency getBase() { return base; }
        public int getRequestedQuotes() { return requestedQuotes; }
    }

    public static final class IngestionRangeSummary {
        private final LocalDate start; private final LocalDate end;
        private int totalInserted; private int totalUpdated;
        public IngestionRangeSummary(LocalDate start, LocalDate end) { this.start = start; this.end = end; }
        public void add(IngestionSummary s) { this.totalInserted += s.getInserted(); this.totalUpdated += s.getUpdated(); }
        public LocalDate getStart() { return start; }
        public LocalDate getEnd() { return end; }
        public int getTotalInserted() { return totalInserted; }
        public int getTotalUpdated() { return totalUpdated; }
    }
}
