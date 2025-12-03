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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private UserRepository userRepository;
    private JwtUtil jwtUtil;
    private AuthenticationManager authenticationManager;
    private SecurityConfig securityConfig;
    private PasswordEncoder passwordEncoder;
    private AuditLogService auditLogService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtUtil = mock(JwtUtil.class);
        authenticationManager = mock(AuthenticationManager.class);
        securityConfig = mock(SecurityConfig.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogService = mock(AuditLogService.class);
        when(securityConfig.passwordEncoder()).thenReturn(passwordEncoder);
        controller = new AuthController(userRepository, jwtUtil, authenticationManager, securityConfig, auditLogService);
    }

    @Test
    void registerUser_success_creates_new_user_with_default_role() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

        ResponseEntity<RegisterResponseDTO> response = controller.registerUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@example.com") &&
                u.getPassword().equals("hashedPassword") &&
                u.getRole() == Role.USER
        ));
    }

    @Test
    void registerUser_duplicate_email_returns_bad_request() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        User existingUser = new User();
        existingUser.setEmail(request.getEmail());
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));

        ResponseEntity<RegisterResponseDTO> response = controller.registerUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_custom_roles_creates_user_with_specified_roles() {
        RegisterRequestDTO request = new RegisterRequestDTO();
        request.setEmail("admin@example.com");
        request.setPassword("password123");
        request.setRoles(List.of("ADMIN"));

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("jwt-token");

        ResponseEntity<RegisterResponseDTO> response = controller.registerUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userRepository).save(argThat(u ->
                u.getRole() == Role.ADMIN
        ));
    }

    @Test
    void loginUser_valid_credentials_returns_jwt_token() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("user@example.com");
        request.setPassword("password123");

        org.springframework.security.core.userdetails.UserDetails userDetails =
                mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetails.getUsername()).thenReturn("user@example.com");
        Collection<? extends GrantedAuthority> authorities = 
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        when(userDetails.getAuthorities()).thenReturn((Collection) authorities);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtUtil.generateToken(any(org.springframework.security.core.userdetails.UserDetails.class)))
                .thenReturn("jwt-token-123");

        ResponseEntity<LoginResponseDTO> response = controller.loginUser(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token-123");
    }
}
