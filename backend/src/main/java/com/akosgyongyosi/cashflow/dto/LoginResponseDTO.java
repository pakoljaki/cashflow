package com.akosgyongyosi.cashflow.dto;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class LoginResponseDTO {
    public LoginResponseDTO(String token2, List<String> roles2) {
        this.token = token2;
        this.roles = roles2;
    }
    private String token;
    private java.util.List<String> roles;
}
