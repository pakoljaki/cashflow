package com.akosgyongyosi.cashflow.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KpiEntry {
    final LocalDate date;
    final BigDecimal amount;
    final String acctCode;
    final String txCategory;
    final boolean positive;

    public KpiEntry(LocalDate date, BigDecimal amount, String acctCode, String txCategory, boolean positive) {
        this.date        = date;
        this.amount      = amount;
        this.acctCode    = acctCode;
        this.txCategory  = txCategory;
        this.positive    = positive;
        }
    }