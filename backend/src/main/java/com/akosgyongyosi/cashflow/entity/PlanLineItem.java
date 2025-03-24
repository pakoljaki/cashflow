package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

// this entity represents the assumption made by the user for the planned cashflow's data
@Entity
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "plan_line_items")
public class PlanLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to parent plan
    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonBackReference
    private CashflowPlan plan;

    @Enumerated(EnumType.STRING)
    private LineItemType type;

    private String title;          // e.g. "New Hire Salary", "Sell Asset"
    private BigDecimal amount;     // can be negative or positive

    private LocalDate transactionDate;  
    // for recurring items:
    // e.g. ONE_TIME, DAILY, WEEKLY, MONTHLY
    @Enumerated(EnumType.STRING)
    private Frequency frequency;  

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private TransactionCategory category;  

    private Double percentChange;  // e.g. +0.10 means +10%
}
