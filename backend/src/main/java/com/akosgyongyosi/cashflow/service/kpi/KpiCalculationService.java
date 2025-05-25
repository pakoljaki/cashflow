package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.*;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.HistoricalTransactionRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiCalculationService {

    private final TransactionRepository txnRepo;
    private final HistoricalTransactionRepository histRepo;
    private final CashflowPlanRepository planRepo;

    @Autowired
    public KpiCalculationService(
            TransactionRepository txnRepo,
            HistoricalTransactionRepository histRepo,
            CashflowPlanRepository planRepo
    ) {
        this.txnRepo = txnRepo;
        this.histRepo = histRepo;
        this.planRepo = planRepo;
    }

    public KpiDashboardDTO calculateForPeriod(
            LocalDate start, LocalDate end, BigDecimal startBalance
    ) {
        List<Transaction> txns = txnRepo.findByBookingDateBetween(start, end);
        List<KpiEntry> entries = txns.stream()
            .map(this::toEntry)
            .collect(Collectors.toList());
        return buildKpi(entries, startBalance != null ? startBalance : BigDecimal.ZERO);
    }

    public KpiDashboardDTO calculateForPlan(Long planId) {
        CashflowPlan plan = planRepo.findById(planId)
            .orElseThrow(() -> new NoSuchElementException("Plan not found: " + planId));
        List<HistoricalTransaction> hist = histRepo.findByCashflowPlanId(planId);
        List<KpiEntry> entries = hist.stream()
            .map(this::toEntry)
            .collect(Collectors.toList());
        BigDecimal sb = plan.getStartBalance() != null ? plan.getStartBalance() : BigDecimal.ZERO;
        return buildKpi(entries, sb);
    }

    private KpiEntry toEntry(Transaction tx) {
        String acct = Optional.ofNullable(tx.getCategory())
            .map(TransactionCategory::getAccountingCategory)
            .map(AccountingCategory::getCode)
            .orElse("");
        String tcat = Optional.ofNullable(tx.getCategory())
            .map(TransactionCategory::getName)
            .orElse("");
        boolean pos = tx.getTransactionDirection() == TransactionDirection.POSITIVE;
        return new KpiEntry(tx.getBookingDate(), tx.getAmount(), acct, tcat, pos);
    }

    private KpiEntry toEntry(HistoricalTransaction ht) {
        String acct = Optional.ofNullable(ht.getCategory())
            .map(TransactionCategory::getAccountingCategory)
            .map(AccountingCategory::getCode)
            .orElse("");
        String tcat = Optional.ofNullable(ht.getCategory())
            .map(TransactionCategory::getName)
            .orElse("");
        boolean pos = Optional.ofNullable(ht.getCategory())
            .map(TransactionCategory::getDirection)
            .map(d -> d == TransactionDirection.POSITIVE)
            .orElse(true);
        return new KpiEntry(ht.getTransactionDate(), ht.getAmount(), acct, tcat, pos);
    }

    private KpiDashboardDTO buildKpi(List<KpiEntry> all, BigDecimal startingBalance) {
        Map<Integer, List<KpiEntry>> byMonth = all.stream()
            .collect(Collectors.groupingBy(e -> e.getDate().getMonthValue()));

        List<MonthlyKpiDTO> months = new ArrayList<>();
        BigDecimal cumBalance = startingBalance;

        for (int m = 1; m <= 12; m++) {
            MonthlyKpiDTO dto = new MonthlyKpiDTO();
            dto.setMonth(m);
            List<KpiEntry> list = byMonth.getOrDefault(m, Collections.emptyList());

            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            Map<String, BigDecimal> acctSums = dto.getAccountingCategorySums();
            Map<String, BigDecimal> txSums   = dto.getTransactionCategorySums();
            Map<String, String> dirs         = dto.getTransactionCategoryDirections();

            list.stream()
                .map(KpiEntry::getTxCategory)
                .distinct()
                .forEach(name -> {
                    boolean p = all.stream()
                        .filter(x -> x.getTxCategory().equals(name))
                        .findFirst()
                        .map(KpiEntry::isPositive)
                        .orElse(true);
                    dirs.put(name, p ? "POSITIVE" : "NEGATIVE");
                });

            for (KpiEntry e : list) {
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                acctSums.merge(e.getAcctCode(), amt, BigDecimal::add);
                txSums.merge(e.getTxCategory(), amt, BigDecimal::add);

                if (e.isPositive()) {
                    income = income.add(amt);
                } else {
                    expense = expense.add(amt);
                }
            }

            dto.setTotalIncome(income);
            dto.setTotalExpense(expense);
            BigDecimal net = income.subtract(expense);
            dto.setNetCashFlow(net);

            cumBalance = cumBalance.add(net);
            dto.setBankBalance(cumBalance);

            months.add(dto);
        }

        KpiDashboardDTO out = new KpiDashboardDTO();
        out.setStartBalance(startingBalance);
        out.setMonthlyData(months);
        out.setTotalRevenue(
            months.stream()
                  .map(MonthlyKpiDTO::getTotalIncome)
                  .reduce(BigDecimal.ZERO, BigDecimal::add)
        );
        out.setTotalExpenses(
            months.stream()
                  .map(MonthlyKpiDTO::getTotalExpense)
                  .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        return out;
    }
}
