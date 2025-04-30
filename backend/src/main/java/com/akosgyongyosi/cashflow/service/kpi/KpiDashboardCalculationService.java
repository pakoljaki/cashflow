package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.HistoricalTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KpiDashboardCalculationService {

    private final HistoricalTransactionRepository historicalTransactionRepository;
    private final CashflowPlanRepository cashflowPlanRepository;

    @Autowired
    public KpiDashboardCalculationService(
            HistoricalTransactionRepository historicalTransactionRepository,
            CashflowPlanRepository cashflowPlanRepository
    ) {
        this.historicalTransactionRepository = historicalTransactionRepository;
        this.cashflowPlanRepository = cashflowPlanRepository;
    }

    public KpiDashboardDTO calculateKpi(Long planId) {
        Optional<CashflowPlan> opt = cashflowPlanRepository.findById(planId);
        if (opt.isEmpty()) return null;
        CashflowPlan plan = opt.get();

        List<HistoricalTransaction> txns = historicalTransactionRepository
                .findByCashflowPlanId(planId);

        Map<String,String> directionMap = txns.stream()
                .map(HistoricalTransaction::getCategory)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(
                    c -> c.getName(),
                    c -> c.getDirection().name()
                ));

        Map<Integer, List<HistoricalTransaction>> byMonth = txns.stream()
            .collect(Collectors.groupingBy(
                t -> t.getTransactionDate().getMonthValue(), 
                () -> new TreeMap<>(), 
                Collectors.toList()
            ));

        BigDecimal cumulative = plan.getStartBalance() == null
            ? BigDecimal.ZERO
            : plan.getStartBalance();

        List<MonthlyKpiDTO> monthly = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            MonthlyKpiDTO dto = new MonthlyKpiDTO();
            dto.setMonth(m);
            dto.getTransactionCategoryDirections().putAll(directionMap);
            
            List<HistoricalTransaction> monthList = 
                byMonth.getOrDefault(m, Collections.emptyList());

            BigDecimal income  = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;

            Map<String, BigDecimal> acctSums = dto.getAccountingCategorySums();
            Map<String, BigDecimal> txnSums  = dto.getTransactionCategorySums();

            for (HistoricalTransaction ht : monthList) {
                BigDecimal amt = ht.getAmount() == null
                    ? BigDecimal.ZERO
                    : ht.getAmount();

                boolean isNegative = ht.getCategory() != null &&
                                     ht.getCategory().getDirection().name().equals("NEGATIVE");
                BigDecimal signed = isNegative ? amt.negate() : amt;

                String code = "";
                if (ht.getCategory() != null 
                 && ht.getCategory().getAccountingCategory() != null) {
                    code = ht.getCategory()
                              .getAccountingCategory()
                              .getCode();
                }
                acctSums.put(code, acctSums
                    .getOrDefault(code, BigDecimal.ZERO)
                    .add(signed));

                String txCat = ht.getCategoryName();
                txnSums.put(txCat, txnSums
                    .getOrDefault(txCat, BigDecimal.ZERO)
                    .add(amt));

                if (signed.compareTo(BigDecimal.ZERO) >= 0) {
                    income = income.add(signed);
                } else {
                    expense = expense.add(signed);
                }
            }

            dto.setTotalIncome(income);
            dto.setTotalExpense(expense);
            BigDecimal net = income.add(expense);
            dto.setNetCashFlow(net);

            cumulative = cumulative.add(net);
            dto.setBankBalance(cumulative);

            monthly.add(dto);
        }

        BigDecimal totalRev = txns.stream()
            .filter(t -> t.getCategory() != null
                      && t.getCategory().getAccountingCategory() != null
                      && List.of("CORE_REV","OTHER_INC")
                         .contains(t.getCategory()
                                    .getAccountingCategory()
                                    .getCode()))
            .filter(t -> !t.getCategory().getDirection().name().equals("NEGATIVE"))
            .map(HistoricalTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCOGS = txns.stream()
            .filter(t -> t.getCategory() != null
                      && t.getCategory().getAccountingCategory() != null
                      && "COGS".equals(t.getCategory()
                                       .getAccountingCategory()
                                       .getCode()))
            .filter(t -> t.getCategory().getDirection().name().equals("NEGATIVE"))
            .map(HistoricalTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOpex = txns.stream()
            .filter(t -> t.getCategory() != null
                      && t.getCategory().getAccountingCategory() != null
                      && "OPEX".equals(t.getCategory()
                                       .getAccountingCategory()
                                       .getCode()))
            .filter(t -> t.getCategory().getDirection().name().equals("NEGATIVE"))
            .map(HistoricalTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDepr = txns.stream()
            .filter(t -> t.getCategory() != null
                      && t.getCategory().getAccountingCategory() != null
                      && "DEPR".equals(t.getCategory()
                                       .getAccountingCategory()
                                       .getCode()))
            .filter(t -> t.getCategory().getDirection().name().equals("NEGATIVE"))
            .map(HistoricalTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ebitda = totalRev.subtract(totalCOGS.abs().add(totalOpex.abs()));
        BigDecimal ebit   = ebitda.subtract(totalDepr.abs());

        BigDecimal profitMargin = BigDecimal.ZERO;
        if (totalRev.compareTo(BigDecimal.ZERO) != 0) {
            profitMargin = ebit.divide(totalRev, 4, RoundingMode.HALF_UP);
        }

        KpiDashboardDTO dash = new KpiDashboardDTO();
        monthly.sort(Comparator.comparingInt(MonthlyKpiDTO::getMonth));
        dash.setMonthlyData(monthly);
        dash.setTotalRevenue(totalRev);
        dash.setTotalExpenses(
            txns.stream()
               .filter(t -> t.getCategory() != null &&
                            t.getCategory().getDirection().name().equals("NEGATIVE"))
               .map(HistoricalTransaction::getAmount)
               .reduce(BigDecimal.ZERO, BigDecimal::add)
               .abs()
        );
        dash.setEbitda(ebitda);
        dash.setEbit(ebit);
        dash.setProfitMargin(profitMargin);

        return dash;
    }
}
