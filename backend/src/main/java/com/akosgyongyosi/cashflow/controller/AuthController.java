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

import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;
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

    public AuthController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager,
                          SecurityConfig securityConfig) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.securityConfig = securityConfig;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> registerUser(@RequestBody RegisterRequestDTO request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RegisterResponseDTO("Email is already taken.", null));
        }

        User user = new User();
        user.setEmail(request.getEmail());
        String rawPassword = request.getPassword();
        String hashedPassword = securityConfig.passwordEncoder().encode(rawPassword);
        user.setPassword(hashedPassword);

        Set<Role> roleSet = new HashSet<>();
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            roleSet.add(Role.USER);
        } else {
            for (String roleStr : request.getRoles()) {
                try {
                    Role role = Role.valueOf(roleStr.trim().toUpperCase());
                    roleSet.add(role);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(new RegisterResponseDTO("Invalid role provided: " + roleStr, null));
                }
            }
        }
        user.setRoles(roleSet);

        userRepository.save(user);
        String token = jwtUtil.generateToken(user.getEmail(), user.getRoles());
        return ResponseEntity.ok(new RegisterResponseDTO("User registered successfully.", token));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        String token = jwtUtil.generateToken(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new LoginResponseDTO(token, roles));
    }
}
