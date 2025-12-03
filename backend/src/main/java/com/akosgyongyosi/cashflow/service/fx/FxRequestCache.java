package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.math.BigDecimal;
import java.util.List;
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
        return convertDetailed(amount, from, to, date).convertedAmount();
    }

   
    public DetailedConversion convertDetailed(BigDecimal amount, Currency from, Currency to, LocalDate date) {
        if (from == to) {
            return new DetailedConversion(amount, List.of(), BigDecimal.ONE);
        }
        final LocalDate effectiveDate = date.isAfter(LocalDate.now()) ? LocalDate.now() : date;
        String key = from.name() + "->" + to.name() + "@" + effectiveDate;
        BigDecimal cross = rateByKey.computeIfAbsent(key,
                k -> fx.convert(BigDecimal.ONE, from, to, effectiveDate)
        );
        return new DetailedConversion(amount.multiply(cross), List.of(), cross);
    }

    public record DetailedConversion(BigDecimal convertedAmount, List<Object> warnings, BigDecimal crossRate) {}
}