package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PlanLineItemDTO {
    private Long id;
    private String type;
    private String category;
    private BigDecimal amount;
    private LocalDate startDate;
    private LocalDate endDate;
    private String frequency;
    private BigDecimal percentageChange;
    private String currency;
}
