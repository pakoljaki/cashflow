package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Setter
@Getter
public class FxSettingsDTO {
    private Currency baseCurrency;
    private String apiBaseUrl;
    private List<Currency> quotes;
    private String refreshCron;
    private boolean enabled;
    private String provider;
}
