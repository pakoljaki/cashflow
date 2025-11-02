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

    private Long assumptionId;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonBackReference
    private CashflowPlan plan;

    @Enumerated(EnumType.STRING)
    private LineItemType type;

    private String title;          
    private BigDecimal amount;
    private LocalDate transactionDate;  

    @Enumerated(EnumType.STRING)
    private Frequency frequency;  

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private TransactionCategory category;  

    private Double percentChange;  
    private Boolean isApplied = false;  // indicates if the line item is applied to the plan

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;
}
