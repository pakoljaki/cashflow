package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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
    // e.g. RECURRING, ONE_TIME, CATEGORY_ADJUSTMENT

    // Common fields for any line item:
    private String title;          // e.g. "New Hire Salary", "Sell Asset"
    private BigDecimal amount;     // can be negative or positive

    // For Recurring items:
    // e.g. monthly, weekly, quarterly, etc.
    @Enumerated(EnumType.STRING)
    private Frequency frequency;   // monthly/quarterly/etc.

    private int startWeek;  // Week-based calculations
    private int endWeek;    // Week-based calculations

    // For category-level adjustments:
    // E.g. "Marketing +10% from June to August"
    @ManyToOne
    @JoinColumn(name = "category_id")
    private TransactionCategory category;  

    private Double percentChange;  // e.g. +0.10 means +10%

    // ...plus more fields as needed

    // Constructors, getters, setters...
}
