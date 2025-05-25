package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import lombok.Data;

import java.math.BigDecimal;


@Data
public class LineItemRequestDTO {
    private LineItemType type;
    private String title;
    private BigDecimal amount;
    private Frequency frequency;
    private Integer startMonth;
    private Integer endMonth;
    private Long categoryId;
    private Double percentChange;
}
