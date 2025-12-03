package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Frequency;
import com.akosgyongyosi.cashflow.entity.LineItemType;
import com.akosgyongyosi.cashflow.entity.Currency;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Data
public class PlanLineItemResponseDTO {
    private Long id;
    private Long assumptionId;
    private String title;
    private LineItemType type;
    private BigDecimal amount;
    private Frequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double percentChange;
    private LocalDate transactionDate;
    private String categoryName;
    private Currency currency;
    private String warning;
    private List<FxWarningDTO> warnings;
    private RateMetaDTO rateMeta;
}
