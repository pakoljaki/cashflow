package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
import com.akosgyongyosi.cashflow.service.fx.FxConversionContext;
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

        if (!item.getIsApplied()) {
            item.setIsApplied(true);
        } else {
            return;
        }

        BigDecimal nativeAmt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
        Currency fromCurrency = item.getCurrency() != null ? item.getCurrency() : FxConversionContext.base();

        for (LocalDate date : transactionDates) {
            HistoricalTransaction newTx = new HistoricalTransaction();
            newTx.setCashflowPlan(plan);
            newTx.setTransactionDate(date);

            BigDecimal amountBase = FxConversionContext.convert(date, fromCurrency, nativeAmt);
            newTx.setAmount(amountBase);
            newTx.setCategory(item.getCategory());

            plan.getBaselineTransactions().add(newTx);
        }
    }

    private List<LocalDate> calculateRecurringDates(CashflowPlan plan, PlanLineItem item) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate startDate = item.getStartDate();
        LocalDate endDate = item.getEndDate();

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
