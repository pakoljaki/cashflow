package com.akosgyongyosi.cashflow.dto;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.Data;

@Getter
@Setter
@Data
public class UpdateCategoryMappingRequestDTO {
    private Long accountingCategoryId;
    private List<Long> transactionCategoryIds;
}