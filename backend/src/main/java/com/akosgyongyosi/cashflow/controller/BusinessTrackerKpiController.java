package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.kpi.KpiCalculationService;
import com.akosgyongyosi.cashflow.service.kpi.KpiDisplayCurrencyConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/business-kpi")
public class BusinessTrackerKpiController {

    private final KpiCalculationService kpiService;
    private final KpiDisplayCurrencyConverter displayConverter;

    @Autowired
    public BusinessTrackerKpiController(KpiCalculationService kpiService,
                                        KpiDisplayCurrencyConverter displayConverter) {
        this.kpiService = kpiService;
        this.displayConverter = displayConverter;
    }

    @GetMapping
    public KpiDashboardDTO getBusinessKpi(@RequestParam String startDate,
                                          @RequestParam String endDate,
                                          @RequestParam(required = false) BigDecimal startBalance,
                                          @RequestParam(required = false) Currency baseCurrency,
                                          @RequestParam(required = false) Currency displayCurrency) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, fmt);
        LocalDate end   = LocalDate.parse(endDate, fmt);
        if (startBalance == null) startBalance = BigDecimal.ZERO;
        Currency base = (baseCurrency != null) ? baseCurrency : Currency.HUF;

        KpiDashboardDTO dash = kpiService.calculateForPeriod(start, end, startBalance, base);
        dash.setBalanceCurrency(base.name());
        
        if (displayCurrency != null && displayCurrency != base) {
            dash = displayConverter.toDisplayCurrency(dash, start, base, displayCurrency);
        }
        return dash;
    }
}
