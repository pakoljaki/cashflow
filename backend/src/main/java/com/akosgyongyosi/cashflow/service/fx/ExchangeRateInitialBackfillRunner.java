package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;


@Component
public class ExchangeRateInitialBackfillRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateInitialBackfillRunner.class);
    private final FxRateEnsurer rateEnsurer;
    private final ExchangeRateRepository repo;
    private final FxProperties props;

    public ExchangeRateInitialBackfillRunner(FxRateEnsurer rateEnsurer,
                                             ExchangeRateRepository repo,
                                             FxProperties props) {
        this.rateEnsurer = rateEnsurer;
        this.repo = repo;
        this.props = props;
    }

    @Override
    public void run(String... args) {
        if (!props.isEnabled()) return;
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(props.getStartupBackfillDays());
        LocalDate end = today.minusDays(1);
        Currency base = props.getCanonicalBase();
        Set<Currency> quotes = new HashSet<>(props.getQuotes());
        quotes.remove(base);

        int missingDays = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean missingAny = false;
            for (Currency q : quotes) {
                if (repo.findByRateDateAndBaseCurrencyAndQuoteCurrency(d, base, q).isEmpty()) { missingAny = true; break; }
            }
            if (missingAny) missingDays++;
        }

        log.info("Initial FX backfill scan {}..{} totalMissingDays={}", start, end, missingDays);
        if (missingDays > props.getWideGapThresholdDays()) {
            log.info("Missing days {} exceed threshold {}; performing initial range ingestion.", missingDays, props.getWideGapThresholdDays());
            try {
                rateEnsurer.ensureForRange(start, end);
            } catch (Exception ex) {
                log.warn("Initial backfill ingestion failed: {}", ex.getMessage());
            }
        } else {
            log.info("Initial backfill skipped; gap threshold not exceeded (threshold={}, missingDays={})", props.getWideGapThresholdDays(), missingDays);
        }
    }
}
