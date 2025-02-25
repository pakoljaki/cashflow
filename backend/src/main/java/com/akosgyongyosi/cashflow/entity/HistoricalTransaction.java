package com.akosgyongyosi.cashflow.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


//we need this so historical data is immutable. We can modify this data when doing the forecasting, assumption making
@Entity
@Getter 
@Setter
@Table(name = "historical_transactions")
public class HistoricalTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate transactionDate;
    
    private BigDecimal amount;
    
    @ManyToOne
    @JoinColumn(name = "category_id")
    @JsonIgnore // Prevents infinite recursion
    private TransactionCategory category;


    private LocalDate snapshotDate; // Date when this snapshot was created

    @ManyToOne
    @JsonBackReference
    private CashflowPlan cashflowPlan; // Link to forecast plan

    // 👇 This will ensure category name is included in the JSON response
    @JsonProperty("category")
    public String getCategoryName() { 
        return category != null ? category.getName() : "Uncategorized";
    }

    // Getters and setters
}
