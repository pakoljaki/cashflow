package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.TransactionDirection;

public class CategoryRequestDTO {
    private String name;
    private TransactionDirection direction;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionDirection getDirection() {
        return direction;
    }

    public void setDirection(TransactionDirection direction) {
        this.direction = direction;
    }
}
