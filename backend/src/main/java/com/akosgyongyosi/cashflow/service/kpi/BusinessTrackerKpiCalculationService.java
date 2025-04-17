package com.akosgyongyosi.cashflow.service.kpi;

import com.akosgyongyosi.cashflow.dto.KpiDashboardDTO;
import com.akosgyongyosi.cashflow.dto.MonthlyKpiDTO;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessTrackerKpiCalculationService {

    private final TransactionRepository transactionRepository;

    @Autowired
    public BusinessTrackerKpiCalculationService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public KpiDashboardDTO calculateKpiForPeriod(LocalDate startDate, LocalDate endDate, BigDecimal startingBalance) {
        List<Transaction> transactions = transactionRepository.findByBookingDateBetween(startDate, endDate);
        Map<Integer, List<Transaction>> transactionsByMonth = transactions.stream().collect(Collectors.groupingBy(tx -> tx.getBookingDate().getMonthValue()));
        List<MonthlyKpiDTO> monthlyList = new ArrayList<>();
        BigDecimal cumulativeBalance = startingBalance != null ? startingBalance : BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            MonthlyKpiDTO dto = new MonthlyKpiDTO();
            dto.setMonth(m);
            List<Transaction> monthTx = transactionsByMonth.getOrDefault(m, new ArrayList<>());
            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;
            Map<String, BigDecimal> accountingSums = dto.getAccountingCategorySums();
            Map<String, BigDecimal> txCategorySums = dto.getTransactionCategorySums();
            for (Transaction tx : monthTx) {
                BigDecimal amt = tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO;
                String acctCode = "";
                String txCat = "";
                if (tx.getCategory() != null) {
                    if (tx.getCategory().getAccountingCategory() != null) {
                        acctCode = tx.getCategory().getAccountingCategory().getCode();
                    }
                    txCat = tx.getCategory().getName();
                }
                accountingSums.put(acctCode, accountingSums.getOrDefault(acctCode, BigDecimal.ZERO).add(amt));
                txCategorySums.put(txCat, txCategorySums.getOrDefault(txCat, BigDecimal.ZERO).add(amt));
                if (tx.getTransactionDirection().equals(TransactionDirection.POSITIVE)) {
                    income = income.add(amt);
                } else {
                    expense = expense.add(amt);
                }
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
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) > 0 &&
                        tx.getCategory() != null &&
                        tx.getCategory().getAccountingCategory() != null &&
                        (tx.getCategory().getAccountingCategory().getCode().equals("CORE_REV") ||
                        tx.getCategory().getAccountingCategory().getCode().equals("OTHER_INC")))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCOGS = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0 &&
                        tx.getCategory() != null &&
                        tx.getCategory().getAccountingCategory() != null &&
                        tx.getCategory().getAccountingCategory().getCode().equals("COGS"))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOpex = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0 &&
                        tx.getCategory() != null &&
                        tx.getCategory().getAccountingCategory() != null &&
                        tx.getCategory().getAccountingCategory().getCode().equals("OPEX"))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDepreciation = transactions.stream()
                .filter(tx -> tx.getAmount().compareTo(BigDecimal.ZERO) < 0 &&
                        tx.getCategory() != null &&
                        tx.getCategory().getAccountingCategory() != null &&
                        tx.getCategory().getAccountingCategory().getCode().equals("DEPR"))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ebitda = totalRevenue.subtract(totalCOGS.add(totalOpex));
        BigDecimal ebit = ebitda.subtract(totalDepreciation);
        BigDecimal profitMargin = BigDecimal.ZERO;
        if (totalRevenue.compareTo(BigDecimal.ZERO) != 0) {
            profitMargin = ebit.divide(totalRevenue, 4, RoundingMode.HALF_UP);
        }
        KpiDashboardDTO dashboard = new KpiDashboardDTO();
        monthlyList.sort(Comparator.comparingInt(MonthlyKpiDTO::getMonth));
        dashboard.setMonthlyData(monthlyList);
        dashboard.setTotalRevenue(totalRevenue);
        BigDecimal totalExpenses = transactions.stream()
                .filter(tx -> tx.getTransactionDirection().equals(TransactionDirection.NEGATIVE))
                .map(tx -> tx.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dashboard.setTotalExpenses(totalExpenses);
        dashboard.setEbitda(ebitda);
        dashboard.setEbit(ebit);
        dashboard.setProfitMargin(profitMargin);
        return dashboard;
    }
}
