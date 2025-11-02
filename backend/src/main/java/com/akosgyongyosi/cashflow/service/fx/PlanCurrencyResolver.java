package com.akosgyongyosi.cashflow.service.fx;

import com.akosgyongyosi.cashflow.entity.CashflowPlan;
import com.akosgyongyosi.cashflow.entity.Currency;

public final class PlanCurrencyResolver {
    private PlanCurrencyResolver() {}

    public static Currency resolve(CashflowPlan plan) {
        if (plan.getBaseCurrency() != null) return plan.getBaseCurrency();
        return Currency.HUF; //TODO
    }
}
