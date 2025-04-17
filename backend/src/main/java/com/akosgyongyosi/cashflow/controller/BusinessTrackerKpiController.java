package com.akosgyongyosi.cashflow.controller;
import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.service.kpi.BusinessTrackerKpiCalculationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
@RestController
@RequestMapping("/api/business-kpi")
public class BusinessTrackerKpiController {
    private final BusinessTrackerKpiCalculationService kpiService;
    @Autowired
    public BusinessTrackerKpiController(BusinessTrackerKpiCalculationService kpiService) {
        this.kpiService = kpiService;
    }
    @GetMapping
    public KpiDashboardDTO getBusinessKpi(@RequestParam String startDate, @RequestParam String endDate, @RequestParam(required=false) BigDecimal startBalance) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        if(startBalance == null) { startBalance = BigDecimal.ZERO; }
        return kpiService.calculateKpiForPeriod(start, end, startBalance);
    }
}
