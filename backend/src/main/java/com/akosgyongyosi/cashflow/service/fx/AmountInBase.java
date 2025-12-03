package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Function;

public final class AmountInBase {
    private AmountInBase() {}

    public static BigDecimal of(LocalDate date,
                                Currency from,
                                BigDecimal amount,
                                Currency base,
                                FxRequestCache cache) {
        if (from == base) return amount;
        return cache.convert(amount, from, base, date);
    }

    public static <T> BigDecimal of(T source,
                                    Function<T, LocalDate> dateGetter,
                                    Function<T, Currency> currencyGetter,
                                    Function<T, BigDecimal> amountGetter,
                                    Currency base,
                                    FxRequestCache cache) {
        LocalDate d = dateGetter.apply(source);
        Currency from = currencyGetter.apply(source);
        BigDecimal amt = amountGetter.apply(source);
        if (from == base) return amt;
        return cache.convert(amt, from, base, d);
    }
}