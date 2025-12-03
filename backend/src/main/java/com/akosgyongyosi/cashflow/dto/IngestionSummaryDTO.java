package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class IngestionSummaryDTO {
    private final LocalDate date;
    private final int inserted;
    private final int updated;
    private final Currency base;
    private final int requestedQuotes;
}
