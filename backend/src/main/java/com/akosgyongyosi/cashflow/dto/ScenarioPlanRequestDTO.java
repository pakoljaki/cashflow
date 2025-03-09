package com.akosgyongyosi.cashflow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ScenarioPlanRequestDTO {
     private String basePlanName;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal startBalance;
}
