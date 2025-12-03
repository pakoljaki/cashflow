package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.config.FxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Set;


@Service
public class TransactionDateRangeFxService {

    private static final Logger log = LoggerFactory.getLogger(TransactionDateRangeFxService.class);

    private final FxRateEnsurer rateEnsurer;
    private final FxProperties props;

    public TransactionDateRangeFxService(FxRateEnsurer rateEnsurer,
                                          FxProperties props) {
        this.rateEnsurer = rateEnsurer;
        this.props = props;
    }

    public void ensureRatesForDateRange(LocalDate startDate, LocalDate endDate) {
        if (!props.isEnabled()) {
            log.debug("FX disabled; skipping rate fetch for date range {}..{}", startDate, endDate);
            return;
        }

        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            log.warn("Invalid date range: {}..{}", startDate, endDate);
            return;
        }

        LocalDate today = LocalDate.now();
        
        LocalDate pastStart = startDate;
        LocalDate pastEnd = endDate.isBefore(today) ? endDate : today.minusDays(1);
        
        if (!pastStart.isAfter(pastEnd)) {
            log.info("Ensuring FX rates for date range {}..{} (historical)", pastStart, pastEnd);
            try {
                rateEnsurer.ensureForRange(pastStart, pastEnd);
                log.info("Successfully ensured rates for historical range {}..{}", pastStart, pastEnd);
            } catch (Exception ex) {
                log.error("Failed to fetch historical rates for range {}..{}: {}", pastStart, pastEnd, ex.getMessage());
            }
        }
        
        if (endDate.isAfter(today)) {
            LocalDate futureStart = startDate.isAfter(today) ? startDate : today;
            log.info("Pre-warming future FX rates for range {}..{} (provisional)", futureStart, endDate);
            
            log.debug("Future rates will be resolved as provisional during conversion");
        }
    }

    public void ensureRatesForTransactionsWithForwardCoverage(Set<LocalDate> transactionDates) {
        if (transactionDates == null || transactionDates.isEmpty()) {
            log.debug("No transaction dates provided");
            return;
        }

        LocalDate minDate = transactionDates.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = transactionDates.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
        
        LocalDate extendedEnd = maxDate.plusYears(1);
        
        log.info("Ensuring FX rates for {} transaction dates (range: {}..{}) + 1 year forward (until {})",
                transactionDates.size(), minDate, maxDate, extendedEnd);
        
        ensureRatesForDateRange(minDate, extendedEnd);
    }

    public void ensureRatesForImportedTransactionDates(LocalDate minDate, LocalDate maxDate) {
        if (minDate == null || maxDate == null) {
            log.debug("No valid date range for import");
            return;
        }
        
        log.info("Ensuring FX rates for CSV import date range: {}..{}", minDate, maxDate);
        ensureRatesForDateRange(minDate, maxDate);
    }
}
