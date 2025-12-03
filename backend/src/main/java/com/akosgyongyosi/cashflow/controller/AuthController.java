package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.config.SecurityConfig;
import com.akosgyongyosi.cashflow.dto.LoginRequestDTO;
import com.akosgyongyosi.cashflow.dto.LoginResponseDTO;
import com.akosgyongyosi.cashflow.dto.RegisterRequestDTO;
import com.akosgyongyosi.cashflow.dto.RegisterResponseDTO;
import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;
import com.akosgyongyosi.cashflow.security.JwtUtil;
import com.akosgyongyosi.cashflow.service.AuditLogService;

import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Import({SecurityConfig.class})
public class AuthController {
    

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final SecurityConfig securityConfig;
    private final AuditLogService auditLogService;

    public AuthController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          SecurityConfig securityConfig,
                          AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.securityConfig = securityConfig;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> registerUser(@RequestBody RegisterRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            auditLogService.logFailedAction(request.getEmail(), "REGISTER_USER", "EMAIL_ALREADY_EXISTS");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RegisterResponseDTO("Email is already taken.", null));
        }

        User user = new User();
        user.setEmail(request.getEmail());
        String rawPassword = request.getPassword();
        String hashedPassword = securityConfig.passwordEncoder().encode(rawPassword);
        user.setPassword(hashedPassword);

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            String roleStr = request.getRoles().get(0);
            try {
                Role role = Role.valueOf(roleStr.trim().toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new RegisterResponseDTO("Invalid role provided: " + roleStr, null));
            }
        }

        userRepository.save(user);
        auditLogService.logAction(user.getEmail(), "REGISTER_USER", 
            java.util.Map.of("role", user.getRole().name()));
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return ResponseEntity.ok(new RegisterResponseDTO("User registered successfully.", token));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        auditLogService.logAction(userDetails.getUsername(), "LOGIN");
        
        String token = jwtUtil.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new LoginResponseDTO(token, roles));
    }
}
