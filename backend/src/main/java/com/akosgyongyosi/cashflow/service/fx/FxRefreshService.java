package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for refreshing exchange rates for a specified number of past days.
 * Used both at startup and for manual admin-triggered refreshes.
 */
@Service
public class FxRefreshService {
    private static final Logger log = LoggerFactory.getLogger(FxRefreshService.class);

    private final FxIngestionService ingestionService;
    private final FxProperties props;
    private final ExchangeRateCache cache;
    private final ExchangeRateRepository exchangeRateRepository;

    public FxRefreshService(FxIngestionService ingestionService, 
                           FxProperties props,
                           ExchangeRateCache cache, 
                           ExchangeRateRepository exchangeRateRepository) {
        this.ingestionService = ingestionService;
        this.props = props;
        this.cache = cache;
        this.exchangeRateRepository = exchangeRateRepository;
    }

   
    public int refreshExchangeRates(int days) {
        log.info("Starting manual FX refresh for past {} days", days);
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate start = today.minusDays(days - 1L);
        
        Currency base = props.getCanonicalBase();
        Set<Currency> quotes = new HashSet<>(props.getQuotes());
        quotes.remove(base);
        
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
            log.info("No missing dates found for past {} days. Reloading cache.", days);
            cache.loadAll(days);
            return 0;
        }
        
        log.info("Found {} missing dates. Fetching each day individually...", missingDates.size());

        List<LocalDate> orderedMissingDates = missingDates.stream()
            .sorted()
            .collect(Collectors.toList());

        int ingestedDays = 0;
        for (LocalDate missingDate : orderedMissingDates) {
            try {
                log.info("Fetching exchange rates for {}", missingDate);
                ingestionService.fetchAndUpsert(missingDate);
                ingestedDays++;
            } catch (Exception ex) {
                log.error("FX ingestion failed for {}: {} -- continuing", missingDate, ex.getMessage());
            }
        }

        log.info("FX refresh completed. Ingested {} days. Reloading cache...", ingestedDays);
        cache.loadAll(days);
        
        return ingestedDays;
    }
}
