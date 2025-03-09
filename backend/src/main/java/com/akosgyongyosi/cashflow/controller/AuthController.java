package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.dto.LoginResponseDTO;
import com.akosgyongyosi.cashflow.dto.RegisterResponseDTO;
import com.akosgyongyosi.cashflow.dto.LoginRequestDTO;
import com.akosgyongyosi.cashflow.dto.RegisterRequestDTO;
import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;
import com.akosgyongyosi.cashflow.security.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public AuthController(UserRepository userRepository,
                          JwtUtil jwtUtil,
                          AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> registerUser(@RequestBody RegisterRequestDTO request) {
        // Check if email is taken
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new RegisterResponseDTO("Email is already taken.", null));
        }

        // Build user entity
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); 
        // convert the incoming list of strings to a set of Role enums
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            // if none provided, default to VIEWER
            user.getRoles().add(Role.VIEWER);
        } else {
            Set<Role> roleSet = request.getRoles().stream()
                .map(String::toUpperCase)      // "admin" -> "ADMIN"
                .map(Role::valueOf)           // "ADMIN" -> Role.ADMIN
                .collect(Collectors.toSet());

            user.setRoles(roleSet);
        }

        userRepository.save(user);

        // Generate JWT with all user roles
        String token = jwtUtil.generateToken(user.getEmail(), user.getRoles());

        // Return a nice response with the token
        return ResponseEntity.ok(
            new RegisterResponseDTO("User registered successfully.", token)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginRequestDTO loginRequest) {
        // Attempt authentication against DB
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), 
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Print roles to verify
        System.out.println("Extracted roles from userDetails: " + userDetails.getAuthorities());

        // Create JWT from userDetails
        String token = jwtUtil.generateToken(userDetails);

        // Extract roles
        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())  
                .collect(Collectors.toList());

        System.out.println("Roles being stored in JWT: " + roles);

        return ResponseEntity.ok(new LoginResponseDTO(token, roles));
    }

}
