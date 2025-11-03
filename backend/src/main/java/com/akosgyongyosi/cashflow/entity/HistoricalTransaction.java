package com.akosgyongyosi.cashflow.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

// we need this so historical data is immutable. We can modify this data when doing the forecasting, assumption making
@Entity
@Getter 
@Setter
@EqualsAndHashCode
@Table(name = "historical_transactions")
public class HistoricalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate transactionDate;

    /**
     * Amount expressed in the plan's base currency (normalized value used for KPI math).
     */
    private BigDecimal amount;

    /**
     * Original raw amount from the source transaction before conversion.
     */
    @Column(name = "original_amount", precision = 18, scale = 4)
    private BigDecimal originalAmount;

    /**
     * Original currency of the source transaction; enables audit / re‑conversion if FX changes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "original_currency", length = 3)
    private Currency originalCurrency;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonIgnore 
    private TransactionCategory category;

    private LocalDate snapshotDate; 

    @ManyToOne
    @JsonBackReference
    private CashflowPlan cashflowPlan; 

    @JsonProperty("category")
    public String getCategoryName() { 
        return category != null ? category.getName() : "Uncategorized";
    }

    @JsonProperty("originalCurrency")
    public String getOriginalCurrencyCode() {
        return originalCurrency != null ? originalCurrency.name() : null;
    }
}
