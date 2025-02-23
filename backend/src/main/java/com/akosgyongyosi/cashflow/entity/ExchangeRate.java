package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Table(name = "exchange_rates")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_currency", nullable = false)
    private CurrencyType fromCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_currency", nullable = false)
    private CurrencyType toCurrency;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal rate;
}
