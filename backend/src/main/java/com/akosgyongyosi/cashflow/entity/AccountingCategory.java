package com.akosgyongyosi.cashflow.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "accounting_categories")
@Getter
@Setter
public class AccountingCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String displayName;
    private String description;

    //I JUST ADDED THIS, use this to determine whether to negate the sum of the accounting sum for the monthly kpi
    @Enumerated(EnumType.STRING)
    @Column(name = "direction") 
    private TransactionDirection direction;
    
    @OneToMany(mappedBy = "accountingCategory")
    @JsonIgnoreProperties("accountingCategory")
    private List<TransactionCategory> transactionCategories;
    
}