package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.TransactionDirection;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequestDTO {
    private String name;
    private TransactionDirection direction;
}
