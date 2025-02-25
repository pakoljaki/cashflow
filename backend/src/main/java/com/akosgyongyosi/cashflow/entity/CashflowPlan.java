package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;


@Entity
@Getter
@Setter
@Table(name = "cashflow_plans")
public class CashflowPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String planName;  

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    // The user-defined assumptions (modifications)
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PlanLineItem> lineItems = new ArrayList<>();

    // NEW: Store the transactions from last year as the "baseline"
    @OneToMany(mappedBy = "cashflowPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<HistoricalTransaction> baselineTransactions = new ArrayList<>();

    

    // Constructors, getters, setters...
}
