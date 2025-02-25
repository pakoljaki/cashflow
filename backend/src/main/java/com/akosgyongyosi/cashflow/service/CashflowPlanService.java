package com.akosgyongyosi.cashflow.service;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.HistoricalTransaction;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.repository.CashflowPlanRepository;
import com.akosgyongyosi.cashflow.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CashflowPlanService {

    private final CashflowPlanRepository planRepository;
    private final TransactionRepository transactionRepository;

    public CashflowPlanService(CashflowPlanRepository planRepository, TransactionRepository transactionRepository) {
        this.planRepository = planRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Create a new cashflow plan.
     */
    public CashflowPlan createPlan(CashflowPlan plan) {
        // You can add validations or default settings here if needed.
        return planRepository.save(plan);
    }

    /**
     * Retrieve a cashflow plan by its ID.
     */
    public Optional<CashflowPlan> getPlan(Long planId) {
        return planRepository.findById(planId);
    }

    /**
     * Convert an immutable Transaction into a HistoricalTransaction snapshot.
     * This method shifts the transaction’s booking date by one year and copies over
     * the amount and category. You can extend this converter to copy additional fields as needed.
     *
     * @param tx The source Transaction.
     * @return A HistoricalTransaction snapshot.
     */
    private HistoricalTransaction convertTransactionToHistorical(Transaction tx) {
        HistoricalTransaction hist = new HistoricalTransaction();
        // Use the bookingDate as the baseline and shift it one year ahead.
        hist.setTransactionDate(tx.getBookingDate().plusYears(1));
        hist.setAmount(tx.getAmount());
        hist.setCategory(tx.getCategory());
        // Optionally copy additional fields (e.g., memo, transactionCode) if needed.
        return hist;
    }

    /**
     * Create a new cashflow plan for the upcoming year based on last year’s transactions.
     * This method retrieves transactions from the previous year, converts them into
     * HistoricalTransaction snapshots, and attaches them to the plan as its baseline.
     *
     * @param planName The name for the new plan.
     * @param start    The start date of the plan.
     * @param end      The end date of the plan.
     * @return The saved CashflowPlan.
     */
    public CashflowPlan createPlanFromLastYear(String planName, LocalDate start, LocalDate end) {
        // 1. Create a new CashflowPlan and set its basic properties.
        CashflowPlan plan = new CashflowPlan();
        plan.setPlanName(planName);
        plan.setStartDate(start);
        plan.setEndDate(end);
        plan.setDescription("Autogenerated from actuals: " + start + " - " + end);

        // 2. Retrieve last year's transactions using the bookingDate property.
        List<Transaction> lastYearTransactions = transactionRepository
            .findByBookingDateBetween(start.minusYears(1), end.minusYears(1));

        // 3. For each transaction, create a historical snapshot and attach it to the plan.
        for (Transaction tx : lastYearTransactions) {
            HistoricalTransaction hist = convertTransactionToHistorical(tx);
            // Link the historical transaction to the plan.
            hist.setCashflowPlan(plan);
            // Add the snapshot to the plan’s baseline collection.
            plan.getBaselineTransactions().add(hist);
        }

        // 4. Save and return the new plan.
        return planRepository.save(plan);
    }
    
    // Additional methods such as updatePlan(...) or deletePlan(...) can be added here.
}
