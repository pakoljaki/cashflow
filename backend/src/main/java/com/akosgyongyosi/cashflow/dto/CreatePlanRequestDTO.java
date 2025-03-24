package com.akosgyongyosi.cashflow.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Data
public class CreatePlanRequestDTO {
    private String planName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal startBalance;
}
