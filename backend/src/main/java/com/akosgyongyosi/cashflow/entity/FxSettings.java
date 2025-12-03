package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name = "fx_settings")
public class FxSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_currency", length = 3, nullable = false)
    private Currency baseCurrency;

    @Column(name = "api_base_url", nullable = false, length = 128)
    private String apiBaseUrl;

    @Column(name = "quotes_csv", nullable = false, length = 512)
    private String quotesCsv;

    @Column(name = "refresh_cron", nullable = false, length = 64)
    private String refreshCron;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "provider", nullable = false, length = 64)
    private String provider;
}
