package com.akosgyongyosi.cashflow.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BulkCategoryRequestDTO {
    private List<Long> transactionIds;
    private Long categoryId;
}
