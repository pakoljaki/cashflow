package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;


@Entity
@Getter
@Setter
@EqualsAndHashCode
@Table(name = "cashflow_plans")
public class CashflowPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String planName;  

    private LocalDate startDate;
    private LocalDate endDate;

    @Column(precision = 15, scale = 2)
    private BigDecimal startBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type")
    private ScenarioType scenario;

    // This identifies which group the plan belongs to
    @Column(name = "group_key", nullable = false)
    private String groupKey;

    @Column(columnDefinition = "TEXT")
    private String description;

    // user-defined assumptions (modifications)
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PlanLineItem> lineItems = new ArrayList<>();

    // store the transactions from last year as the "baseline"
    @OneToMany(mappedBy = "cashflowPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<HistoricalTransaction> baselineTransactions = new ArrayList<>();

    
}
