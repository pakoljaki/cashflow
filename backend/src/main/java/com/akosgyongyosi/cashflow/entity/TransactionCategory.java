package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = "transaction_categories")
public class TransactionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // example: "Salary", "Office Rent", "Marketing"

    // Boolean inflationFollowing;
    @ManyToOne
    @JoinColumn(name = "accounting_category_id")
    private AccountingCategory accountingCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction") 
    private TransactionDirection direction;


    @Column(name = "description")
    private String description; // optional: explanation of category

    @OneToMany(mappedBy = "category")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<Transaction> transactions;
}
