package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.LineItemType;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Data
public class PlanLineItemRequestDTO {
    private String title;
    private LineItemType type;
    private BigDecimal amount;
    private Frequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double percentChange;
    private LocalDate transactionDate;
    private Long categoryId;
    private Long assumptionId;
}
