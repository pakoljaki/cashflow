package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;


@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = "transactions")
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount account;

    @Column(nullable = false)
    private LocalDate bookingDate; 

    @Column(nullable = false)
    private LocalDate valueDate; 

    @Column(name = "partner_name")
    private String partnerName; 

    @Column(name = "partner_account_number")
    private String partnerAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyType currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionDirection transactionDirection;

    @Column(name = "transaction_code")
    private String transactionCode; 

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = true)
    private TransactionCategory category;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_method", nullable = false)
    private TransactionMethod transactionMethod;

    // boolen ignore in plan, its a one-off transaction

}
