package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transaction_categories")
public class TransactionCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // Example: "Salary", "Office Rent", "Marketing"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionDirection direction; // POSITIVE (jóváírás) or NEGATIVE (terhelés)

    @Column(name = "description")
    private String description; // Optional: explanation of category

    @OneToMany(mappedBy = "category")
    private List<Transaction> transactions;
}
