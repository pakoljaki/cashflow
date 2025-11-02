package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;

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

    public Long getId() { return id; }
    public Currency getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(Currency baseCurrency) { this.baseCurrency = baseCurrency; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public String getQuotesCsv() { return quotesCsv; }
    public void setQuotesCsv(String quotesCsv) { this.quotesCsv = quotesCsv; }
    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
