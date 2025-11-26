package com.akosgyongyosi.cashflow.dto;

import java.util.List;

public class BulkCategoryRequestDTO {
    private List<Long> transactionIds;
    private Long categoryId;

    public List<Long> getTransactionIds() {
        return transactionIds;
    }

    public void setTransactionIds(List<Long> transactionIds) {
        this.transactionIds = transactionIds;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
