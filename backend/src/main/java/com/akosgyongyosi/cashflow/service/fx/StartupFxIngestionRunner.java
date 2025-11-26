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
import java.util.HashSet;
import java.util.Set;

@Component
public class StartupFxIngestionRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupFxIngestionRunner.class);

    private final FxIngestionService ingestionService;
    private final FxProperties props;
    private final ExchangeRateCache cache;
    private final ExchangeRateRepository exchangeRateRepository;

    public StartupFxIngestionRunner(FxIngestionService ingestionService, FxProperties props, 
                                   ExchangeRateCache cache, ExchangeRateRepository exchangeRateRepository) {
        this.ingestionService = ingestionService; 
        this.props = props; 
        this.cache = cache;
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
        Set<Currency> quotes = new HashSet<>(props.getQuotes());
        quotes.remove(base);
        
        long totalRecords = exchangeRateRepository.count();
        log.info("Exchange rate database contains {} records", totalRecords);
        
        LocalDate mostRecentDate = null;
        for (Currency quote : quotes) {
            LocalDate latest = exchangeRateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(base, quote)
                .map(rate -> rate.getRateDate())
                .orElse(null);
            
            if (latest != null && (mostRecentDate == null || latest.isAfter(mostRecentDate))) {
                mostRecentDate = latest;
            }
        }
        
        log.info("Most recent exchange rate date: {}", mostRecentDate);
        
        if (totalRecords >= 1000 && mostRecentDate != null && !mostRecentDate.isBefore(yesterday)) {
            log.info("✓ Exchange rate data is up-to-date (count={}, mostRecent={}). Skipping ingestion, loading cache only.", 
                totalRecords, mostRecentDate);
            int days = props.getStartupBackfillDays();
            cache.loadAll(days);
            return;
        }
        
        log.info("⚠ Exchange rate data needs update (count={}, mostRecent={}). Finding missing dates...", 
            totalRecords, mostRecentDate);
        
        int days = props.getStartupBackfillDays();
        LocalDate start = today.minusDays(days - 1L);
        
        Set<LocalDate> missingDates = new HashSet<>();
        for (LocalDate date = start; !date.isAfter(yesterday); date = date.plusDays(1)) {
            boolean hasAllRates = true;
            for (Currency quote : quotes) {
                if (exchangeRateRepository.findByRateDateAndBaseCurrencyAndQuoteCurrency(date, base, quote).isEmpty()) {
                    hasAllRates = false;
                    break;
                }
            }
            if (!hasAllRates) {
                missingDates.add(date);
            }
        }
        
        if (missingDates.isEmpty()) {
            log.info("✓ No missing dates found. Loading cache.");
            cache.loadAll(days);
            return;
        }
        
        log.info("Found {} missing dates. Fetching exchange rates in chunks...", missingDates.size());
        
        int chunk = props.getChunkSizeDays();
        int ingestedWindows = 0;
        LocalDate cursor = start;
        
        while (!cursor.isAfter(yesterday)) {
            LocalDate windowEnd = cursor.plusDays(chunk - 1L);
            if (windowEnd.isAfter(yesterday)) windowEnd = yesterday;
            
            LocalDate finalCursor = cursor;
            LocalDate finalWindowEnd = windowEnd;
            boolean windowHasMissing = missingDates.stream()
                .anyMatch(d -> !d.isBefore(finalCursor) && !d.isAfter(finalWindowEnd));
            
            if (windowHasMissing) {
                try {
                    log.info("Fetching exchange rates for window {}..{}", cursor, windowEnd);
                    ingestionService.fetchAndUpsert(cursor, windowEnd);
                    ingestedWindows++;
                } catch (Exception ex) {
                    log.error("FX ingestion window {}..{} failed: {} -- continuing", cursor, windowEnd, ex.getMessage());
                }
            }
            
            cursor = windowEnd.plusDays(1);
        }
        
        log.info("FX startup ingestion completed. Ingested {} windows covering missing dates. Loading cache...", ingestedWindows);
        cache.loadAll(days);
        
        if (!props.isDynamicFetchEnabled()) {
            log.info("Dynamic FX fetch disabled; system operating in cache-only mode.");
        } else {
            log.info("Dynamic FX fetch enabled (fx.dynamicFetchEnabled=true); set to false for strict cache-only mode.");
        }
    }
}
