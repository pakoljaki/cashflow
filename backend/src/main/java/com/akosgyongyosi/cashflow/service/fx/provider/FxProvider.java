package com.akosgyongyosi.cashflow.service.fx.provider;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public interface FxProvider {
    Map<Currency, BigDecimal> getDailyQuotes(LocalDate date, Currency base, Set<Currency> quotes);
    String getProviderName();
    /**
     * Optional optimized range fetch. Implementations may return an empty map to signal unsupported,
     * in which case caller should fall back to per-day getDailyQuotes.
     * Structure: date -> (quoteCurrency -> rate)
     */
    default Map<LocalDate, Map<Currency, BigDecimal>> getRangeQuotes(LocalDate start, LocalDate end, Currency base, Set<Currency> quotes) {
        return Map.of();
    }
}
