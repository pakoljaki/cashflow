package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class RecurringTransactionStrategy implements ForecastStrategy {

    @Override
    public boolean supports(LineItemType type) {
        return type == LineItemType.RECURRING;
    }

    @Override
    public void applyForecast(CashflowPlan plan, PlanLineItem item) {
        List<LocalDate> transactionDates = calculateRecurringDates(plan, item);
        
        for (LocalDate date : transactionDates) {
            HistoricalTransaction newTx = new HistoricalTransaction();
            newTx.setCashflowPlan(plan);
            newTx.setTransactionDate(date);
            newTx.setAmount(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            newTx.setCategory(item.getCategory());
            plan.getBaselineTransactions().add(newTx);
        }
    }

    private List<LocalDate> calculateRecurringDates(CashflowPlan plan, PlanLineItem item) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate startDate = plan.getStartDate().plusWeeks(item.getStartWeek());
        LocalDate endDate = plan.getStartDate().plusWeeks(item.getEndWeek());

        switch (item.getFrequency()) {
            case WEEKLY:
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusWeeks(1)) {
                    dates.add(date);
                }
                break;
            case BI_WEEKLY:
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusWeeks(2)) {
                    dates.add(date);
                }
                break;
            case MONTHLY:
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusMonths(1)) {
                    dates.add(date);
                }
                break;
            case QUARTERLY:
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusMonths(3)) {
                    dates.add(date);
                }
                break;
            case SEMI_ANNUAL:
                for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusMonths(6)) {
                    dates.add(date);
                }
                break;
            case ANNUAL:
                dates.add(startDate);
                break;
            default:
                throw new IllegalArgumentException("Unsupported frequency: " + item.getFrequency());
        }
        return dates;
    }
}
