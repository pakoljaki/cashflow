package com.akosgyongyosi.cashflow.security;

import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_shouldLoadUserWithSingleRole() {
        String email = "user@example.com";
        User user = createUser(email, "password123", Set.of(Role.USER));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(email);
        assertThat(result.getPassword()).isEqualTo("password123");
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactly("ROLE_USER");
        
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_shouldLoadUserWithMultipleRoles() {
        String email = "admin@example.com";
        User user = createUser(email, "adminpass", Set.of(Role.USER, Role.ADMIN));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorities()).hasSize(2);
        assertThat(result.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_shouldThrowExceptionWhenUserNotFound() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessageContaining("User not found: " + email);
        
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_shouldHandleUserWithNoRoles() {
        String email = "noroles@example.com";
        User user = createUser(email, "password", new HashSet<>());

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result).isNotNull();
        assertThat(result.getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_shouldPrefixRolesWithROLE() {
        String email = "user@example.com";
        User user = createUser(email, "password", Set.of(Role.USER));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result.getAuthorities())
            .extracting(GrantedAuthority::getAuthority)
            .allMatch(auth -> auth.startsWith("ROLE_"));
    }

    @Test
    void loadUserByUsername_shouldHandleDifferentEmailFormats() {
        String email = "User.Name+tag@Example.COM";
        User user = createUser(email, "password", Set.of(Role.USER));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result.getUsername()).isEqualTo(email);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsername_shouldCreateAccountEnabledByDefault() {
        String email = "user@example.com";
        User user = createUser(email, "password", Set.of(Role.USER));

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(email);

        assertThat(result.isEnabled()).isTrue();
        assertThat(result.isAccountNonExpired()).isTrue();
        assertThat(result.isAccountNonLocked()).isTrue();
        assertThat(result.isCredentialsNonExpired()).isTrue();
    }

    private User createUser(String email, String password, Set<Role> roles) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setRoles(roles);
        return user;
    }
}
