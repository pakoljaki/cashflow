package com.akosgyongyosi.cashflow.repository;

import com.akosgyongyosi.cashflow.entity.Currency;
import com.akosgyongyosi.cashflow.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByRateDateAndBaseCurrencyAndQuoteCurrency(
            LocalDate rateDate, Currency base, Currency quote);

    Optional<ExchangeRate>
    findTopByBaseCurrencyAndQuoteCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            Currency base, Currency quote, LocalDate rateDate);

    Optional<ExchangeRate> findTopByBaseCurrencyAndQuoteCurrencyOrderByRateDateDesc(
            Currency base, Currency quote);

    Optional<ExchangeRate>
    findTopByBaseCurrencyAndQuoteCurrencyAndRateDateGreaterThanEqualOrderByRateDateAsc(
            Currency base, Currency quote, LocalDate rateDate);
}