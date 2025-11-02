package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class FxConversionContext {

    public record Ctx(Currency base, FxRequestCache cache) {}

    private static final ThreadLocal<Ctx> CTX = new ThreadLocal<>();

    private FxConversionContext() {}

    public static void open(Currency base, FxRequestCache cache) {
        CTX.set(new Ctx(base, cache));
    }

    public static void close() {
        CTX.remove();
    }

    public static Ctx current() {
        Ctx c = CTX.get();
        if (c == null) throw new IllegalStateException("FxConversionContext not initialized");
        return c;
    }

    public static BigDecimal convert(LocalDate date, Currency from, BigDecimal amount) {
        Ctx c = current();
        if (from == c.base()) return amount;
        return c.cache().convert(amount, from, c.base(), date);
    }

    public static Currency base() {
        return current().base();
    }
}
