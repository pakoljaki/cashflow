package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.service.fx.FxRequestCache;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class KpiDisplayCurrencyConverter {

    private final FxService fxService;

    public KpiDisplayCurrencyConverter(FxService fxService) {
        this.fxService = fxService;
    }

    public KpiDashboardDTO toDisplayCurrency(KpiDashboardDTO src,
                                             LocalDate periodStart,
                                             Currency base,
                                             Currency display) {
        if (display == null || display == base) return src;

        FxRequestCache cache = new FxRequestCache(fxService);

    LocalDate startConvDate = periodStart.minusDays(1);
    src.setOriginalStartBalance(src.getStartBalance());
    src.setStartBalanceRateDate(startConvDate.toString());
    src.setStartBalanceRateSource("ECB/Frankfurter");
    src.setBaseCurrency(base.name());
    src.setDisplayCurrency(display.name());
    src.setStartBalance(cache.convert(src.getStartBalance(), base, display, startConvDate));

        int year = periodStart.getYear();
        for (MonthlyKpiDTO m : src.getMonthlyData()) {
            LocalDate monthStart = LocalDate.of(year, m.getMonth(), 1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

            var rateDate = monthEnd; 
            m.setOriginalTotalIncome(m.getTotalIncome());
            m.setOriginalTotalExpense(m.getTotalExpense());
            m.setOriginalNetCashFlow(m.getNetCashFlow());
            m.setOriginalBankBalance(m.getBankBalance());
            m.setRateDate(rateDate.toString());
            m.setRateSource("ECB/Frankfurter");
            m.setTotalIncome(cache.convert(m.getTotalIncome(), base, display, rateDate));
            m.setTotalExpense(cache.convert(m.getTotalExpense(), base, display, rateDate));
            m.setNetCashFlow(cache.convert(m.getNetCashFlow(), base, display, rateDate));
            m.setBankBalance(cache.convert(m.getBankBalance(), base, display, rateDate));

            convertMap(m.getAccountingCategorySums(), base, display, monthEnd, cache);
            convertMap(m.getTransactionCategorySums(), base, display, monthEnd, cache);
        }

        BigDecimal totalRevenue = src.getMonthlyData().stream()
                .map(MonthlyKpiDTO::getTotalIncome).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = src.getMonthlyData().stream()
                .map(MonthlyKpiDTO::getTotalExpense).reduce(BigDecimal.ZERO, BigDecimal::add);
        src.setTotalRevenue(totalRevenue);
        src.setTotalExpenses(totalExpenses);

        return src;
    }

    private void convertMap(Map<String, BigDecimal> map,
                            Currency from, Currency to,
                            LocalDate date,
                            FxRequestCache cache) {
        for (Map.Entry<String, BigDecimal> e : map.entrySet()) {
            e.setValue(cache.convert(e.getValue(), from, to, date));
        }
    }
}
