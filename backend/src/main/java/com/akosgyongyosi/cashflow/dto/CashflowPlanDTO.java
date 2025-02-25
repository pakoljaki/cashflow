package com.akosgyongyosi.cashflow.dto;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CashflowPlanDTO {
    
    private Long id;
    
    private String planName;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private String description;
    
    private List<PlanLineItemDTO> lineItems;

    // Constructor, Getters, Setters
}
