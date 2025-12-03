package com.akosgyongyosi.cashflow.dto;

import com.akosgyongyosi.cashflow.entity.Currency;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Data
public class ConvertedTxDTO {
    LocalDate date;
    BigDecimal amountBase;       
    Currency baseCurrency;          
    Long categoryId;
    Long accountId;
}
