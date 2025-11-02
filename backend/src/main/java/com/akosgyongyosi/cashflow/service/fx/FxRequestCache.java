package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public final class FxRequestCache {
    private final FxService fx;
    private final Map<String, BigDecimal> rateByKey = new HashMap<>();

    public FxRequestCache(FxService fx) {
        this.fx = fx;
    }

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from == to) return amount;
        String key = from.name() + "->" + to.name() + "@" + date;
        BigDecimal rate = rateByKey.computeIfAbsent(key,
                k -> fx.convert(BigDecimal.ONE, from, to, date) // exact per-day cross rate
        );
        return amount.multiply(rate);
    }
}