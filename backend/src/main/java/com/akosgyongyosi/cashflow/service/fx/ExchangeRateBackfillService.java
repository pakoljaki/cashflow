package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Component
public class ExchangeRateBackfillService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateBackfillService.class);

    private final FxRateEnsurer rateEnsurer;
    private final ExchangeRateRepository repo;
    private final FxProperties props;

    public ExchangeRateBackfillService(FxRateEnsurer rateEnsurer,
                                       ExchangeRateRepository repo,
                                       FxProperties props) {
        this.rateEnsurer = rateEnsurer;
        this.repo = repo;
        this.props = props;
    }

    // Run at 03:12 every day (after typical ECB update) — cron: second minute hour day month dow
    @Scheduled(cron = "0 12 3 * * ?")
    public void dailyBackfill() {
        if (!props.isEnabled() || !props.isDynamicFetchEnabled()) return; // disabled in cache-only mode
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(props.getDailyBackfillDays());
        LocalDate end = today.minusDays(1); // exclude today (still provisional ingest elsewhere)
        backfillRange(start, end, false);
    }

    private void backfillRange(LocalDate startInclusive, LocalDate endInclusive, boolean wide) {
        Currency base = props.getCanonicalBase();
        Set<Currency> quotes = new HashSet<>(props.getQuotes());
        quotes.remove(base);
        int missingDays = 0;
        for (LocalDate d = startInclusive; !d.isAfter(endInclusive); d = d.plusDays(1)) {
            boolean anyMissing = false;
            for (Currency q : quotes) {
                if (repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(d, base, q).isEmpty()) {
                    anyMissing = true; break;
                }
            }
            if (anyMissing && props.isDynamicFetchEnabled()) {
                missingDays++;
                try {
                    rateEnsurer.ensureFor(d);
                } catch (Exception ex) {
                    log.warn("Backfill ingestion failed for {}: {}", d, ex.getMessage());
                }
            }
        }
        log.info("FX backfill {}..{} missingDays={} wide={} base={} quotes={}", startInclusive, endInclusive, missingDays, wide, base, quotes.size());

        if (!wide && props.isDynamicFetchEnabled() && missingDays > props.getWideGapThresholdDays()) {
            LocalDate wideStart = LocalDate.now().minusDays(props.getStartupBackfillDays());
            LocalDate wideEnd = LocalDate.now().minusDays(1);
            log.info("Detected {} missing days (>7). Initiating wide catch-up {}..{}", missingDays, wideStart, wideEnd);
            backfillRange(wideStart, wideEnd, true);
        }
    }
}
