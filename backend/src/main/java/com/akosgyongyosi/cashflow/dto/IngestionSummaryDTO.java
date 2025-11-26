package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;

import java.time.LocalDate;

public class IngestionSummaryDTO {
    private final LocalDate date;
    private final int inserted;
    private final int updated;
    private final Currency base;
    private final int requestedQuotes;

    public IngestionSummaryDTO(LocalDate date, int inserted, int updated, Currency base, int requestedQuotes) {
        this.date = date;
        this.inserted = inserted;
        this.updated = updated;
        this.base = base;
        this.requestedQuotes = requestedQuotes;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getInserted() {
        return inserted;
    }

    public int getUpdated() {
        return updated;
    }

    public Currency getBase() {
        return base;
    }

    public int getRequestedQuotes() {
        return requestedQuotes;
    }
}
