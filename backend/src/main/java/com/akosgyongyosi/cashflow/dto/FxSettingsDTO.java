package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.util.List;

public class FxSettingsDTO {
    private Currency baseCurrency;
    private String apiBaseUrl;
    private List<Currency> quotes;
    private String refreshCron;
    private boolean enabled;
    private String provider;

    public Currency getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(Currency baseCurrency) { this.baseCurrency = baseCurrency; }
    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
    public List<Currency> getQuotes() { return quotes; }
    public void setQuotes(List<Currency> quotes) { this.quotes = quotes; }
    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
