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
    
    @OneToMany(mappedBy = "accountingCategory")
    @JsonIgnoreProperties("accountingCategory")
    private List<TransactionCategory> transactionCategories;
    
}