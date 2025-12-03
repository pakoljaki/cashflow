package com.akosgyongyosi.cashflow.dto;


public enum FxWarningCode {
    FUTURE_DATE_FALLBACK,      
    PROVISIONAL_RATE,          
    MISSING_RATE_FETCHED,     
    STALE_RATE,                
    HISTORICAL_GAP_FALLBACK,   
    HISTORICAL_GAP_TODAY_FALLBACK,
    EXTERNAL_SERVICE_FAILURE  
}
