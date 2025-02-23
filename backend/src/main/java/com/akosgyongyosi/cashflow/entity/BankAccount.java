package com.akosgyongyosi.cashflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_accounts")
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber; //if accountnumber (IN THE 5TH COLUMN)was not provided in the csv file, dont try to create an account for it in that row. in a nutshell: if accountnumber is null, skip the row (starting with HU)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurrencyType currency; //SET IT BASED ON PARSED FILE NAME, HUF if HUF or EUR if EUR

    @Column(name = "bank_name")
    private String bankName; //only set it for the created bankaccount based on the transaction details if it is provided

    @Column(name = "owner")
    private String owner; //in the 3rd column in the csv file its our company's bank account number, its always the same in the whole file doesnt change ever, so its enought to read it in the first line, its always there. On the other hand if you want to store the account for the other person/business in the transaction (5th column) you have to make sure that your csv reading can also handle cases where col5 is empty, and in that case you have to set the owner to null.
}
