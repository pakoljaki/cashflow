package com.akosgyongyosi.cashflow.service.kpi;
import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.HistoricalTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class KpiDashboardCalculationService {
    private final HistoricalTransactionRepository historicalTransactionRepository;
    private final CashflowPlanRepository cashflowPlanRepository;
    @Autowired
    public KpiDashboardCalculationService(HistoricalTransactionRepository historicalTransactionRepository, CashflowPlanRepository cashflowPlanRepository) {
        this.historicalTransactionRepository = historicalTransactionRepository;
        this.cashflowPlanRepository = cashflowPlanRepository;
    }
    public KpiDashboardDTO calculateKpi(Long planId) {
        CashflowPlan plan = cashflowPlanRepository.findById(planId).orElse(null);
        if(plan == null) { return null; }
        List<HistoricalTransaction> transactions = historicalTransactionRepository.findByCashflowPlanId(planId);
        Map<Integer, List<HistoricalTransaction>> transactionsByMonth = transactions.stream().collect(Collectors.groupingBy(tx -> tx.getTransactionDate().getMonthValue()));
        List<MonthlyKpiDTO> monthlyList = new ArrayList<>();
        BigDecimal cumulativeBalance = plan.getStartBalance() != null ? plan.getStartBalance() : BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            MonthlyKpiDTO dto = new MonthlyKpiDTO();
            dto.setMonth(m);
            List<HistoricalTransaction> monthTx = transactionsByMonth.getOrDefault(m, new ArrayList<>());
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            Map<String, BigDecimal> categorySums = dto.getAccountingCategorySums();
            for(HistoricalTransaction tx : monthTx) {
                BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                String code = "";
                if(tx.getCategory() != null && tx.getCategory().getAccountingCategory() != null) {
                    code = tx.getCategory().getAccountingCategory().getCode();
                }
                categorySums.put(code, categorySums.getOrDefault(code, BigDecimal.ZERO).add(amt));
                if(amt.compareTo(BigDecimal.ZERO) > 0) { income = income.add(amt); }
                else { expense = expense.add(amt); }
            }
            dto.setTotalIncome(income);
            dto.setTotalExpense(expense);
            BigDecimal net = income.add(expense);
            dto.setNetCashFlow(net);
            cumulativeBalance = cumulativeBalance.add(net);
            dto.setBankBalance(cumulativeBalance);
            monthlyList.add(dto);
        }
        BigDecimal totalRevenue = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) > 0 && tx.getCategory() != null && tx.getCategory().getAccountingCategory() != null &&
                        (tx.getCategory().getAccountingCategory().getCode().equals("CORE_REV") || tx.getCategory().getAccountingCategory().getCode().equals("OTHER_INC")))
                .map(HistoricalTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCOGS = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0 && tx.getCategory() != null && tx.getCategory().getAccountingCategory() != null &&
                        tx.getCategory().getAccountingCategory().getCode().equals("COGS"))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOpex = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0 && tx.getCategory() != null && tx.getCategory().getAccountingCategory() != null &&
                        tx.getCategory().getAccountingCategory().getCode().equals("OPEX"))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDepreciation = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0 && tx.getCategory() != null && tx.getCategory().getAccountingCategory() != null &&
                        tx.getCategory().getAccountingCategory().getCode().equals("DEPR"))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ebitda = totalRevenue.subtract(totalCOGS.add(totalOpex));
        BigDecimal ebit = ebitda.subtract(totalDepreciation);
        BigDecimal profitMargin = BigDecimal.ZERO;
        if(totalRevenue.compareTo(BigDecimal.ZERO) != 0) { profitMargin = ebit.divide(totalRevenue, 4, RoundingMode.HALF_UP); }
        KpiDashboardDTO dashboard = new KpiDashboardDTO();
        monthlyList.sort(Comparator.comparingInt(MonthlyKpiDTO::getMonth));
        dashboard.setMonthlyData(monthlyList);
        dashboard.setTotalRevenue(totalRevenue);
        dashboard.setTotalExpenses(transactions.stream().filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0).map(tx -> tx.getAmount().abs()).reduce(BigDecimal.ZERO, BigDecimal::add));
        dashboard.setEbitda(ebitda);
        dashboard.setEbit(ebit);
        dashboard.setProfitMargin(profitMargin);
        return dashboard;
    }
}
