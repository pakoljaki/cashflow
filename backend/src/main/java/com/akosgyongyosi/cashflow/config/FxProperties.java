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
    // Number of past days to cover on initial application startup.
    private int initialBackfillDays = 365; // legacy field; use startupBackfillDays for new refactor
    // Number of recent past days to ensure daily via scheduler.
    private int dailyBackfillDays = 30;
    // Threshold of missing days in daily window that triggers wide catch-up.
    private int wideGapThresholdDays = 7;
    private int forwardWarmDays = 90;
    // --- New Refactor Properties ---
    // Perform one-time ingestion of this many past days at startup; disable all dynamic fetches afterwards.
    private int startupBackfillDays = 1000; // ~3 years
    // Size of chunks (in days) when performing large historical ingestion to avoid provider timeouts.
    private int chunkSizeDays = 120; // 4 months
    // Disable any dynamic / on-demand external provider calls when false.
    private boolean dynamicFetchEnabled = false; // default false: only startup ingestion; enable true to allow runtime fetches
    // HTTP timing knobs (ms) for provider calls
    private int httpConnectTimeoutMs = 5000;
    private int httpReadTimeoutMs = 15000;
    private int httpMaxAttempts = 3;

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

    public int getInitialBackfillDays() { return initialBackfillDays; }
    public void setInitialBackfillDays(int initialBackfillDays) { this.initialBackfillDays = initialBackfillDays; }

    public int getDailyBackfillDays() { return dailyBackfillDays; }
    public void setDailyBackfillDays(int dailyBackfillDays) { this.dailyBackfillDays = dailyBackfillDays; }

    public int getWideGapThresholdDays() { return wideGapThresholdDays; }
    public void setWideGapThresholdDays(int wideGapThresholdDays) { this.wideGapThresholdDays = wideGapThresholdDays; }
    public int getForwardWarmDays() { return forwardWarmDays; }
    public void setForwardWarmDays(int forwardWarmDays) { this.forwardWarmDays = forwardWarmDays; }

    public int getStartupBackfillDays() { return startupBackfillDays; }
    public void setStartupBackfillDays(int startupBackfillDays) { this.startupBackfillDays = startupBackfillDays; }
    public int getChunkSizeDays() { return chunkSizeDays; }
    public void setChunkSizeDays(int chunkSizeDays) { this.chunkSizeDays = chunkSizeDays; }
    public boolean isDynamicFetchEnabled() { return dynamicFetchEnabled; }
    public void setDynamicFetchEnabled(boolean dynamicFetchEnabled) { this.dynamicFetchEnabled = dynamicFetchEnabled; }
    public int getHttpConnectTimeoutMs() { return httpConnectTimeoutMs; }
    public void setHttpConnectTimeoutMs(int httpConnectTimeoutMs) { this.httpConnectTimeoutMs = httpConnectTimeoutMs; }
    public int getHttpReadTimeoutMs() { return httpReadTimeoutMs; }
    public void setHttpReadTimeoutMs(int httpReadTimeoutMs) { this.httpReadTimeoutMs = httpReadTimeoutMs; }
    public int getHttpMaxAttempts() { return httpMaxAttempts; }
    public void setHttpMaxAttempts(int httpMaxAttempts) { this.httpMaxAttempts = httpMaxAttempts; }
}
