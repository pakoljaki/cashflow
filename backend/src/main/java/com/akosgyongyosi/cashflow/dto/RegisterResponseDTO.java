package com.akosgyongyosi.cashflow.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class RegisterResponseDTO {
    public RegisterResponseDTO(String string, Object object) {
        //TODO Auto-generated constructor stub
    }
    private String message;
    private String token;
}
