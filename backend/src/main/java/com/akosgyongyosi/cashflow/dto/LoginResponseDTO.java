package com.akosgyongyosi.cashflow.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private java.util.List<String> roles;
}
