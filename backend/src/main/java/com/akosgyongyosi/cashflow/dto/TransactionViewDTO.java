package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.Transaction;
import com.akosgyongyosi.cashflow.entity.TransactionCategory;
import com.akosgyongyosi.cashflow.entity.TransactionDirection;
import com.akosgyongyosi.cashflow.entity.TransactionMethod;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionViewDTO(
        Long id,
        LocalDate bookingDate,
        LocalDate valueDate,
        String partnerName,
        String partnerAccountNumber,
        BigDecimal amount,
        Currency currency,
        TransactionDirection transactionDirection,
        String transactionCode,
        String memo,
        TransactionMethod transactionMethod,
        TransactionCategory category,
        BigDecimal convertedAmount,
        Currency displayCurrency,
        LocalDate rateDate,
        String rateSource
) {

    public static TransactionViewDTO from(Transaction tx) {
        return new TransactionViewDTO(
                tx.getId(),
                tx.getBookingDate(),
                tx.getValueDate(),
                tx.getPartnerName(),
                tx.getPartnerAccountNumber(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getTransactionDirection(),
                tx.getTransactionCode(),
                tx.getMemo(),
                tx.getTransactionMethod(),
                tx.getCategory(),
                null,
                null,
                null,
                null
        );
    }

    public static TransactionViewDTO withConversion(Transaction tx,
                                                    BigDecimal convertedAmount,
                                                    Currency displayCurrency,
                                                    LocalDate rateDate,
                                                    String rateSource) {
        return new TransactionViewDTO(
                tx.getId(),
                tx.getBookingDate(),
                tx.getValueDate(),
                tx.getPartnerName(),
                tx.getPartnerAccountNumber(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getTransactionDirection(),
                tx.getTransactionCode(),
                tx.getMemo(),
                tx.getTransactionMethod(),
                tx.getCategory(),
                convertedAmount,
                displayCurrency,
                rateDate,
                rateSource
        );
    }
}
