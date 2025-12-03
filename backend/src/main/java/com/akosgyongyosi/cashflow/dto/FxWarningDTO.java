package com.akosgyongyosi.cashflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FxWarningDTO {
    private FxWarningCode code;
    private String message;
    private String severity; 
}
