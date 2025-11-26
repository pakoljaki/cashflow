package com.akosgyongyosi.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured FX warning returned with plan line items or transaction conversions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxWarningDTO {
    private FxWarningCode code;
    private String message;
    private String severity; // INFO | WARN | ERROR (string for simpler JSON mapping)
}
