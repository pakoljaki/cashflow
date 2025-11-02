package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.HistoricalTransactionRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import com.akosgyongyosi.cashflow.service.fx.AmountInBase;
import com.akosgyongyosi.cashflow.service.fx.FxRequestCache;
import com.akosgyongyosi.cashflow.service.fx.PlanCurrencyResolver;
import com.akosgyongyosi.cashflow.service.fx.FxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiCalculationService {

    private final TransactionRepository transactionRepository;
    private final HistoricalTransactionRepository historicalTransactionRepository;
    private final CashflowPlanRepository cashflowPlanRepository;
    private final FxService fxService;

    @Autowired
    public KpiCalculationService(TransactionRepository transactionRepository,
                                 HistoricalTransactionRepository historicalTransactionRepository,
                                 CashflowPlanRepository cashflowPlanRepository,
                                 FxService fxService) {
        this.transactionRepository = transactionRepository;
        this.historicalTransactionRepository = historicalTransactionRepository;
        this.cashflowPlanRepository = cashflowPlanRepository;
        this.fxService = fxService;
    }

    // Backwards-compatible overload if old callers exist; defaults to HUF
    public KpiDashboardDTO calculateForPeriod(LocalDate start, LocalDate end, BigDecimal startBalance) {
        return calculateForPeriod(start, end, startBalance, Currency.HUF);
    }

    public KpiDashboardDTO calculateForPeriod(LocalDate start, LocalDate end, BigDecimal startBalance, Currency baseCurrency) {
        FxRequestCache cache = new FxRequestCache(fxService);
        List<Transaction> txs = transactionRepository.findByBookingDateBetween(start, end);
        List<KpiEntry> entries = txs.stream()
                .map(tx -> toEntry(tx, baseCurrency, cache))
                .collect(Collectors.toList());
        BigDecimal opening = startBalance != null ? startBalance : BigDecimal.ZERO;
        return build(entries, opening);
    }

    public KpiDashboardDTO calculateForPlan(Long planId) {
        CashflowPlan plan = cashflowPlanRepository.findById(planId)
                .orElseThrow(() -> new NoSuchElementException("Plan not found: " + planId));
        Currency base = PlanCurrencyResolver.resolve(plan);
        FxRequestCache cache = new FxRequestCache(fxService);
        List<HistoricalTransaction> hist = historicalTransactionRepository.findByCashflowPlanId(planId);
        List<KpiEntry> entries = hist.stream().map(this::toEntry).collect(Collectors.toList());
        BigDecimal opening = plan.getStartBalance() != null ? plan.getStartBalance() : BigDecimal.ZERO;
        return build(entries, opening);
    }

    private KpiEntry toEntry(Transaction tx, Currency base, FxRequestCache cache) {
        String acct = Optional.ofNullable(tx.getCategory())
                .map(TransactionCategory::getAccountingCategory)
                .map(AccountingCategory::getCode)
                .orElse("");
        String tcat = Optional.ofNullable(tx.getCategory()).map(TransactionCategory::getName).orElse("");
        boolean pos = Optional.ofNullable(tx.getCategory())
                .map(TransactionCategory::getDirection)
                .map(d -> d == TransactionDirection.POSITIVE)
                .orElse(true);
        BigDecimal amt = AmountInBase.of(tx, Transaction::getBookingDate, Transaction::getCurrency, Transaction::getAmount, base, cache);
        return new KpiEntry(tx.getBookingDate(), amt, acct, tcat, pos);
    }

    private KpiEntry toEntry(HistoricalTransaction ht) {
        String acct = Optional.ofNullable(ht.getCategory())
                .map(TransactionCategory::getAccountingCategory)
                .map(AccountingCategory::getCode)
                .orElse("");
        String tcat = Optional.ofNullable(ht.getCategory()).map(TransactionCategory::getName).orElse("");
        boolean pos = Optional.ofNullable(ht.getCategory())
                .map(TransactionCategory::getDirection)
                .map(d -> d == TransactionDirection.POSITIVE)
                .orElse(true);
        return new KpiEntry(ht.getTransactionDate(), ht.getAmount(), acct, tcat, pos);
    }

    private KpiDashboardDTO build(List<KpiEntry> all, BigDecimal startBalance) {
        Map<Integer, List<KpiEntry>> byMonth = all.stream()
                .collect(Collectors.groupingBy(e -> e.getDate().getMonthValue()));
        List<MonthlyKpiDTO> monthly = new ArrayList<>();
        BigDecimal runningBank = startBalance;

        for (int m = 1; m <= 12; m++) {
            List<KpiEntry> list = byMonth.getOrDefault(m, Collections.emptyList());
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;

            MonthlyKpiDTO dto = new MonthlyKpiDTO();
            dto.setMonth(m);

            for (KpiEntry e : list) {
                if (e.isPositive()) {
                    income = income.add(e.getAmount());
                } else {
                    expense = expense.add(e.getAmount());
                }
                dto.getAccountingCategorySums().merge(e.getAcctCode(), e.getAmount(), BigDecimal::add);
                dto.getTransactionCategorySums().merge(e.getTxCategory(), e.getAmount(), BigDecimal::add);
                dto.getTransactionCategoryDirections().putIfAbsent(
                        e.getTxCategory(), e.isPositive() ? "POSITIVE" : "NEGATIVE");
            }

            dto.setTotalIncome(income);
            dto.setTotalExpense(expense);
            dto.setNetCashFlow(income.subtract(expense));
            runningBank = runningBank.add(dto.getNetCashFlow());
            dto.setBankBalance(runningBank);

            monthly.add(dto);
        }

        KpiDashboardDTO out = new KpiDashboardDTO();
        out.setStartBalance(startBalance);
        out.setMonthlyData(monthly);
        out.setTotalRevenue(monthly.stream().map(MonthlyKpiDTO::getTotalIncome)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        out.setTotalExpenses(monthly.stream().map(MonthlyKpiDTO::getTotalExpense)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return out;
    }
}
