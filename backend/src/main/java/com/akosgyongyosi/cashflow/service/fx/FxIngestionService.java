package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.dto.IngestionRangeSummaryDTO;
import com.akosgyongyosi.cashflow.dto.IngestionSummaryDTO;
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
    public IngestionSummaryDTO fetchAndUpsert(LocalDate date) {
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
            return new IngestionSummaryDTO(date, inserted, updated, base, quotes.size());
        } catch (Exception ex) {
            long f = FAILURES.incrementAndGet();
            log.error("FX ingest failed for {} (failures={}): {}", date, f, ex.getMessage(), ex);
            throw ex;
        }
    }

    @Transactional
    public IngestionRangeSummaryDTO fetchAndUpsert(LocalDate startInclusive, LocalDate endInclusive) {
        IngestionRangeSummaryDTO sum = new IngestionRangeSummaryDTO(startInclusive, endInclusive);
        Currency base = props.getCanonicalBase();
        Set<Currency> quotes = props.getQuotes();
        Map<LocalDate, Map<Currency, BigDecimal>> range = provider.getRangeQuotes(startInclusive, endInclusive, base, quotes);
        if (range != null && !range.isEmpty()) {
            for (Map.Entry<LocalDate, Map<Currency, BigDecimal>> day : range.entrySet()) {
                LocalDate date = day.getKey();
                Map<Currency, BigDecimal> dayRates = day.getValue();
                int inserted = 0; int updated = 0;
                for (Currency q : quotes) {
                    if (q == base) continue;
                    BigDecimal rate = dayRates.get(q);
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
                    if (isNew) { inserted++; INSERTED.incrementAndGet(); } else { updated++; UPDATED.incrementAndGet(); }
                }
                sum.add(new IngestionSummaryDTO(date, inserted, updated, base, quotes.size()));
            }
            log.info("FX ingest (range optimized) {}..{} -> inserted={} updated={}", startInclusive, endInclusive, sum.getTotalInserted(), sum.getTotalUpdated());
            warnIfStale();
            return sum;
        }
        for (LocalDate d = startInclusive; !d.isAfter(endInclusive); d = d.plusDays(1)) {
            sum.add(fetchAndUpsert(d));
        }
        log.info("FX ingest (range loop) {}..{} -> inserted={} updated={}", startInclusive, endInclusive, sum.getTotalInserted(), sum.getTotalUpdated());
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

}
