package com.akosgyongyosi.cashflow.config;

import com.akosgyongyosi.cashflow.entity.Currency;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix="fx")
public class FxProperties {
    private Currency canonicalBase = Currency.EUR;
    private String apiBaseUrl = "https://api.frankfurter.app";
    private Set<Currency> quotes = EnumSet.of(Currency.HUF, Currency.USD);
    private String refreshCron = "0 10 6 * * *";
    private boolean enabled = true;
    private int stalenessWarnDays = 5;

    public Currency getCanonicalBase() { return canonicalBase; }
    public void setCanonicalBase(Currency canonicalBase) { this.canonicalBase = canonicalBase; }

    public String getApiBaseUrl() { return apiBaseUrl; }
    public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }

    public Set<Currency> getQuotes() { return quotes; }
    public void setQuotes(Set<Currency> quotes) { this.quotes = quotes; }

    public String getRefreshCron() { return refreshCron; }
    public void setRefreshCron(String refreshCron) { this.refreshCron = refreshCron; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getStalenessWarnDays() { return stalenessWarnDays; }
    public void setStalenessWarnDays(int stalenessWarnDays) { this.stalenessWarnDays = stalenessWarnDays; }
}
