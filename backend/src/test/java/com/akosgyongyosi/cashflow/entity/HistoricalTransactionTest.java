package com.akosgyongyosi.cashflow.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HistoricalTransactionTest {

    @Test
    void getCategoryName_returnsName() {
        TransactionCategory cat = new TransactionCategory();
        cat.setName("Utilities");
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(cat);
        assertThat(tx.getCategoryName()).isEqualTo("Utilities");
    }

    @Test
    void getCategoryName_returnsUncategorized() {
        HistoricalTransaction tx = new HistoricalTransaction();
        tx.setCategory(null);
        assertThat(tx.getCategoryName()).isEqualTo("Uncategorized");
    }
}
