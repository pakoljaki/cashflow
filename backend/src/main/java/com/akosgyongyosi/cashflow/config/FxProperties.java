package com.akosgyongyosi.cashflow.config;

import com.akosgyongyosi.cashflow.entity.Currency;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix="fx")
public class FxProperties {
    private Currency canonicalBase = Currency.EUR;
    private String apiBaseUrl = "https://api.frankfurter.app";
    private Set<Currency> quotes = EnumSet.of(Currency.HUF, Currency.USD);
    private String refreshCron = "0 10 6 * * *";
    private boolean enabled = true;
    private int stalenessWarnDays = 5;
    private int dailyBackfillDays = 30;
    private int wideGapThresholdDays = 7;
    private int forwardWarmDays = 90;
    private int startupBackfillDays = 1100; // >3 years
    private int chunkSizeDays = 120; 
    private boolean dynamicFetchEnabled = false; 
    private int httpConnectTimeoutMs = 5000;
    private int httpReadTimeoutMs = 15000;
    private int httpMaxAttempts = 3;
}
