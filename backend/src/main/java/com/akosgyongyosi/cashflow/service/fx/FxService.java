package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import com.akosgyongyosi.cashflow.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class FxService {

    private final ExchangeRateRepository repo;
    private static final Currency CANONICAL_BASE = Currency.EUR;

    public FxService(ExchangeRateRepository repo) {
        this.repo = repo;
    }

    /** Returns CANONICAL_BASE -> quote rate on or before the given date */
    private BigDecimal baseTo(Currency quote, LocalDate date) {
        if (quote == CANONICAL_BASE) return BigDecimal.ONE;

        ExchangeRate er = repo
                .findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                        CANONICAL_BASE, quote, date)
                .orElseThrow(() ->
                        new IllegalStateException("No FX rate for " + CANONICAL_BASE + "->" + quote + " on/before " + date));

        return er.getRateMid();
    }

    /** Cross-rate: from→to = (BASE→to) / (BASE→from), evaluated at 'date' */
    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from == to) return amount;
        BigDecimal baseToTo   = baseTo(to, date);
        BigDecimal baseToFrom = baseTo(from, date);
        return amount.multiply(baseToTo).divide(baseToFrom, 8, RoundingMode.HALF_UP);
    }
}