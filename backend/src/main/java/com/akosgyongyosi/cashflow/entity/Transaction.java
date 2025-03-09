package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.akosgyongyosi.cashflow.entity.TransactionDirection.NEGATIVE;
import static com.akosgyongyosi.cashflow.entity.TransactionDirection.POSITIVE;

import lombok.EqualsAndHashCode;
/*import static com.akosgyongyosi.cashflow.entity.TransactionType.CASH;
import static com.akosgyongyosi.cashflow.entity.TransactionType.TRANSFER;
import static com.akosgyongyosi.cashflow.entity.TransactionType.UNKNOWN;*/
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonInclude;


@Getter
@Setter
@Entity
@EqualsAndHashCode
@Table(name = "transactions")
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
    private TransactionDirection transactionDirection; // POSITIVE or NEGATIVE

    @Column(name = "transaction_code")
    private String transactionCode; // MKB-s tranzakciótípus kivonati kód

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = true)
    private TransactionCategory category; // categorization of transactions

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_method", nullable = false)
    private TransactionMethod transactionMethod;
    
    public Transaction() {}

    public Transaction(BankAccount account, LocalDate bookingDate, LocalDate valueDate, String partnerName, 
                       String partnerAccountNumber, BigDecimal amount, CurrencyType currency,
                       TransactionDirection transactionDirection, String transactionCode, TransactionCategory category, String memo) {
        this.account = account;
        this.bookingDate = bookingDate;
        this.valueDate = valueDate;
        this.partnerName = partnerName;
        this.partnerAccountNumber = partnerAccountNumber;
        this.amount = amount;
        this.currency = currency;
        this.transactionDirection = transactionDirection;
        this.transactionCode = transactionCode;
        this.category = category;
        this.memo = memo;
    }
}
