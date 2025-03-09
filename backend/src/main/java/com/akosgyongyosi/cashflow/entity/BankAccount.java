package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyType currency; // set based on parsed files name 

    @Column(name = "bank_name")
    private String bankName; 

    @Column(name = "owner")
    private String owner;
}
