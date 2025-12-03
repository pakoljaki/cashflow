package com.akosgyongyosi.cashflow.service.forecast.strategy;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.PlanLineItem;
import com.akosgyongyosi.cashflow.service.forecast.ForecastStrategy;
import com.akosgyongyosi.cashflow.service.fx.FxConversionContext;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class RecurringTransactionStrategy implements ForecastStrategy {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionStrategy.class);

    public RecurringTransactionStrategy(FxService fxService) {
    }

    @Override
    public boolean supports(LineItemType type) {
        return type == LineItemType.RECURRING;
    }

    @Override
    public void applyForecast(CashflowPlan plan, PlanLineItem item) {
        log.debug("RecurringStrategy BEGIN itemId={} title='{}' currency={} isApplied={}", item.getId(), item.getTitle(), item.getCurrency(), item.getIsApplied());

        if (item.getStartDate() == null || item.getEndDate() == null || item.getFrequency() == null) {
            log.warn("RecurringStrategy MISSING_FIELDS itemId={} startDate={} endDate={} frequency={} -> SKIP", item.getId(), item.getStartDate(), item.getEndDate(), item.getFrequency());
            return; 
        }

        if (Boolean.TRUE.equals(item.getIsApplied())) {
            log.debug("RecurringStrategy SKIP_ALREADY_APPLIED itemId={}", item.getId());
            return;
        }

        List<LocalDate> transactionDates;
        try {
            transactionDates = calculateRecurringDates(item);
        } catch (Exception ex) {
            log.error("RecurringStrategy DATE_CALC_ERROR itemId={} message={}", item.getId(), ex.getMessage());
            return; 
        }

        if (transactionDates.isEmpty()) {
            log.warn("RecurringStrategy NO_DATES itemId={} startDate={} endDate={} frequency={}", item.getId(), item.getStartDate(), item.getEndDate(), item.getFrequency());
            return;
        }

        BigDecimal nativeAmt = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
        Currency fromCurrency = item.getCurrency() != null ? item.getCurrency() : FxConversionContext.base();

        int success = 0;
        for (LocalDate date : transactionDates) {
            if (date == null) { 
                log.warn("RecurringStrategy NULL_DATE itemId={} frequency={} listSize={}", item.getId(), item.getFrequency(), transactionDates.size());
                continue;
            }
            try {
                HistoricalTransaction newTx = new HistoricalTransaction();
                newTx.setCashflowPlan(plan);
                newTx.setTransactionDate(date);
                BigDecimal amountBase = FxConversionContext.convert(date, fromCurrency, nativeAmt);
                newTx.setAmount(amountBase);
                newTx.setOriginalAmount(nativeAmt);
                newTx.setOriginalCurrency(fromCurrency);
                newTx.setCategory(item.getCategory());
                plan.getBaselineTransactions().add(newTx);
                success++;
                log.trace("RecurringStrategy TX_CREATED itemId={} date={} origCur={} origAmt={} baseAmt={}", item.getId(), date, fromCurrency, nativeAmt, amountBase);
            } catch (Exception ex) {
                log.error("RecurringStrategy TX_ERROR itemId={} date={} message={}", item.getId(), date, ex.getMessage());
            }
        }

        if (success > 0) {
            item.setIsApplied(true); 
            log.debug("RecurringStrategy COMPLETE itemId={} successTx={} totalPlanned={}", item.getId(), success, transactionDates.size());
        } else {
            log.warn("RecurringStrategy NO_SUCCESS itemId={} plannedDates={}", item.getId(), transactionDates.size());
        }
    }

    private List<LocalDate> calculateRecurringDates(PlanLineItem item) {
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
