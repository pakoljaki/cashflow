package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class FxService {

    private final ExchangeRateRepository repo;
    private final FxRateEnsurer ensurer;
    private static final Currency CANONICAL_BASE = Currency.EUR;
    private static final Logger log = LoggerFactory.getLogger(FxService.class);

    public FxService(ExchangeRateRepository repo, FxRateEnsurer ensurer) {
        this.repo = repo;
        this.ensurer = ensurer;
    }

    /** Returns CANONICAL_BASE -> quote rate on or before the given date */
    private BigDecimal baseTo(Currency quote, LocalDate date) {
        if (quote == CANONICAL_BASE) return BigDecimal.ONE;

        var optBefore = repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            CANONICAL_BASE, quote, date);

        if (optBefore.isEmpty()) {
            // Attempt to fetch rates for this date synchronously (best-effort) and re-query.
            try {
                log.debug("No FX rate found for {}->{} on/before {}, attempting on-demand ingestion", CANONICAL_BASE, quote, date);
                ensurer.ensureFor(date);
            } catch (Exception e) {
                log.warn("On-demand FX ingestion failed for {}: {}", date, e.getMessage());
            }
            optBefore = repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                    CANONICAL_BASE, quote, date);
        }

        if (optBefore.isPresent()) {
            return optBefore.get().getRateMid();
        }

        // No prior rate available. Try to find the first rate on/after the date.
        var optAfter = repo.findTopByBaseCurrencyAndQuoteCurrencyAndRateDateGreaterThanEqualOrderByRateDateAsc(
                CANONICAL_BASE, quote, date);

        if (optAfter.isPresent()) {
            log.warn("Using later FX rate for {}->{} dated {} (requested {}) because no prior rate was available",
                    CANONICAL_BASE, quote, optAfter.get().getRateDate(), date);
            return optAfter.get().getRateMid();
        }

        // As a last resort, use the most recent available rate (if any).
        var optAny = repo.findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(CANONICAL_BASE, quote);
        if (optAny.isPresent()) {
            log.warn("No nearby FX rate for {}->{} around {} - using latest available dated {}",
                    CANONICAL_BASE, quote, date, optAny.get().getRateDate());
            return optAny.get().getRateMid();
        }

        throw new IllegalStateException("No FX rate for " + CANONICAL_BASE + "->" + quote + " on/before " + date);
    }

    /** Cross-rate: from→to = (BASE→to) / (BASE→from), evaluated at 'date' */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from == to) return amount;
        BigDecimal baseToTo   = baseTo(to, date);
        BigDecimal baseToFrom = baseTo(from, date);
        return amount.multiply(baseToTo).divide(baseToFrom, 8, RoundingMode.HALF_UP);
    }
}