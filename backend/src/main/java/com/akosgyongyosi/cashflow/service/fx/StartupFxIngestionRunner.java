package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;


@Component
public class StartupFxIngestionRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupFxIngestionRunner.class);

    private final FxRefreshService fxRefreshService;
    private final FxProperties props;
    private final ExchangeRateRepository exchangeRateRepository;

    public StartupFxIngestionRunner(FxRefreshService fxRefreshService,
                                   FxProperties props,
                                   ExchangeRateRepository exchangeRateRepository) {
        this.fxRefreshService = fxRefreshService;
        this.props = props;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(50)
    public void onReady() {
        if (!props.isEnabled()) {
            log.info("FX subsystem disabled; skipping startup ingestion.");
            return;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Currency base = props.getCanonicalBase();
        Set<Currency> quotes = Set.copyOf(props.getQuotes());
        
        long totalRecords = exchangeRateRepository.count();
        log.info("Exchange rate database contains {} records", totalRecords);
        
        LocalDate mostRecentDate = null;
        for (Currency quote : quotes) {
            if (quote.equals(base)) continue;
            
            LocalDate latest = exchangeRateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(base, quote)
                .map(rate -> rate.getRateDate())
                .orElse(null);
            
            if (latest != null && (mostRecentDate == null || latest.isAfter(mostRecentDate))) {
                mostRecentDate = latest;
            }
        }
        
        log.info("Most recent exchange rate date: {}", mostRecentDate);
        
        int days = props.getStartupBackfillDays();
        
        if (totalRecords >= 1000 && mostRecentDate != null && !mostRecentDate.isBefore(yesterday)) {
            log.info("✓ Exchange rate data is up-to-date (count={}, mostRecent={}). Loading cache only.", 
                totalRecords, mostRecentDate);
            logOperatingMode();
            return;
        }
        
        log.info("⚠ Exchange rate data needs update (count={}, mostRecent={}). Starting refresh...", 
            totalRecords, mostRecentDate);
        
        fxRefreshService.refreshExchangeRates(days);
        logOperatingMode();
    }
    
    private void logOperatingMode() {
        if (!props.isDynamicFetchEnabled()) {
            log.info("✓ Operating in CACHE-ONLY mode (dynamic fetch disabled). All exchange rates served from cache.");
        } else {
            log.info("✓ Operating in DYNAMIC-FETCH mode (dynamic fetch enabled). Missing exchange rates will be fetched on-demand.");
        }
    }
}
