package com.akosgyongyosi.cashflow.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class KpiEntry {
    final LocalDate date;
    final BigDecimal amount;
    final String acctCode;
    final String txCategory;
    final boolean positive;
}