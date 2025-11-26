package com.akosgyongyosi.cashflow.dto;

import java.time.LocalDate;

public class IngestionRangeSummaryDTO {
    private final LocalDate start;
    private final LocalDate end;
    private int totalInserted;
    private int totalUpdated;

    public IngestionRangeSummaryDTO(LocalDate start, LocalDate end) {
        this.start = start;
        this.end = end;
    }

    public void add(IngestionSummaryDTO summary) {
        if (summary == null) {
            return;
        }
        this.totalInserted += summary.getInserted();
        this.totalUpdated += summary.getUpdated();
    }

    public LocalDate getStart() {
        return start;
    }

    public LocalDate getEnd() {
        return end;
    }

    public int getTotalInserted() {
        return totalInserted;
    }

    public int getTotalUpdated() {
        return totalUpdated;
    }
}
