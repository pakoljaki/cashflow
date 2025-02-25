package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class LineItemRequestDTO {
    private LineItemType type;       // RECURRING / ONE_TIME / CATEGORY_ADJUSTMENT
    private String title;
    private BigDecimal amount;
    private Frequency frequency;     // monthly, weekly, ...
    private Integer startMonth;
    private Integer endMonth;
    private Long categoryId;         // optional
    private Double percentChange;    // optional, for category adjustments
}
