package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AmountInBaseTest {

    private FxRequestCache cache;

    @BeforeEach
    void setUp() {
        FxService fxService = mock(FxService.class);
        when(fxService.convert(BigDecimal.ONE, Currency.EUR, Currency.HUF, LocalDate.of(2024, 12, 1)))
                .thenReturn(BigDecimal.valueOf(400));
        cache = new FxRequestCache(fxService);
    }

    @Test
    void of_simple_same_currency_returns_unchanged() {
        BigDecimal amount = BigDecimal.valueOf(100);
        LocalDate date = LocalDate.of(2024, 12, 1);

        BigDecimal result = AmountInBase.of(date, Currency.HUF, amount, Currency.HUF, cache);

        assertThat(result).isEqualByComparingTo(amount);
    }

    @Test
    void of_simple_converts_using_cache() {
        BigDecimal amount = BigDecimal.valueOf(10);
        LocalDate date = LocalDate.of(2024, 12, 1);

        BigDecimal result = AmountInBase.of(date, Currency.EUR, amount, Currency.HUF, cache);

        assertThat(result).isEqualByComparingTo("4000"); // 10 * 400
    }

    @Test
    void of_entity_style_uses_accessors() {
        TestEntity entity = new TestEntity(LocalDate.of(2024, 12, 1), Currency.EUR, BigDecimal.valueOf(5));

        BigDecimal result = AmountInBase.of(
                entity,
                TestEntity::getDate,
                TestEntity::getCurrency,
                TestEntity::getAmount,
                Currency.HUF,
                cache);

        assertThat(result).isEqualByComparingTo("2000"); // 5 * 400
    }

    @Test
    void of_entity_style_same_currency_returns_unchanged() {
        TestEntity entity = new TestEntity(LocalDate.of(2024, 12, 1), Currency.HUF, BigDecimal.valueOf(1000));

        BigDecimal result = AmountInBase.of(
                entity,
                TestEntity::getDate,
                TestEntity::getCurrency,
                TestEntity::getAmount,
                Currency.HUF,
                cache);

        assertThat(result).isEqualByComparingTo("1000");
    }

    private static class TestEntity {
        private final LocalDate date;
        private final Currency currency;
        private final BigDecimal amount;

        TestEntity(LocalDate date, Currency currency, BigDecimal amount) {
            this.date = date;
            this.currency = currency;
            this.amount = amount;
        }

        LocalDate getDate() {
            return date;
        }

        Currency getCurrency() {
            return currency;
        }

        BigDecimal getAmount() {
            return amount;
        }
    }
}
