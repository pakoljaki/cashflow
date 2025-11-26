package com.akosgyongyosi.cashflow.dto;

/**
 * Enumerates FX related warning situations returned to the frontend so the UI can surface
 * transparency about how an amount was converted.
 */
public enum FxWarningCode {
    FUTURE_DATE_FALLBACK,      // Requested booking date > today; used today's rate instead.
    PROVISIONAL_RATE,          // Rate stored as provisional (future date) – subject to later reconciliation.
    MISSING_RATE_FETCHED,      // Rate was not cached locally and had to be fetched on-demand now.
    STALE_RATE,                // Latest known rate older than staleness threshold.
    HISTORICAL_GAP_FALLBACK,   // Exact historical date missing; fell back to earlier available date.
    HISTORICAL_GAP_TODAY_FALLBACK, // Past date completely missing; fell back all the way to today's rate.
    EXTERNAL_SERVICE_FAILURE   // External provider unreachable; used last known fallback.
}
