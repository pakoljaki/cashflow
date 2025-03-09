package com.akosgyongyosi.cashflow.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class RegisterResponseDTO {
    private String message;
    private String token;

    public RegisterResponseDTO(String message, String token) {
        this.message = message;
        this.token = token;
    }
}
