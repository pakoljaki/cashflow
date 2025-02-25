package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PlanLineItemDTO {
    
    private Long id;

    private String type; // "RECURRING", "ONE_TIME", "CATEGORY_ADJUSTMENT"

    private String category;

    private BigDecimal amount;

    private LocalDate startDate;

    private LocalDate endDate;

    private String frequency; // "WEEKLY", "MONTHLY", "QUARTERLY"

    private BigDecimal percentageChange; // For category adjustments (e.g., +10%)

    // Constructor, Getters, Setters
}
