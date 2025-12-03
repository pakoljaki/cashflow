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

    private BigDecimal amount;

    @Column(name = "original_amount", precision = 18, scale = 4)
    private BigDecimal originalAmount;

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
