package com.akosgyongyosi.cashflow.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class LoginResponseDTO {
    private String token;
    private java.util.List<String> roles;

    public LoginResponseDTO(String token, java.util.List<String> roles) {
        this.token = token;
        this.roles = roles;
    }
}
