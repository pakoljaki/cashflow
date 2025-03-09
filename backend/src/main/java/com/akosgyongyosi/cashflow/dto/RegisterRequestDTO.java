package com.akosgyongyosi.cashflow.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Data
public class RegisterRequestDTO {
    private String email;
    private String password;
    private java.util.List<String> roles; // e.g. ["ADMIN", "VIEWER"]
}
