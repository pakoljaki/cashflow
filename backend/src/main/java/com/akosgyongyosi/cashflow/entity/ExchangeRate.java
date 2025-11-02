package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@EqualsAndHashCode
@Entity
@Table(
        name = "exchange_rate",
        uniqueConstraints = @UniqueConstraint(
                name = "ux_exchangerate_date_base_quote",
                columnNames = {"rate_date", "base_currency", "quote_currency"}
        ),
        indexes = {
                @Index(name = "ix_exchangerate_base_quote_date", columnList = "base_currency, quote_currency, rate_date")
        }
)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", length = 3, nullable = false)
    private Currency baseCurrency; // e.g., EUR (canonical base)

    @Enumerated(EnumType.STRING)
    @Column(name = "quote_currency", length = 3, nullable = false)
    private Currency quoteCurrency; // e.g., HUF, USD

    @Column(name = "rate_mid", precision = 18, scale = 8, nullable = false)
    private BigDecimal rateMid; // base -> quote mid

    @Column(name = "provider", length = 32, nullable = false)
    private String provider; // "ECB", "MNB", etc.

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt = Instant.now();

}
