package com.akosgyongyosi.cashflow.security;

import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Used by Spring Security to authenticate a user
     * (e.g., from AuthController -> authenticationManager).
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Convert user roles to Spring Security authorities
        var authorities = user.getRoles().stream()
            .map(Role::name)                // e.g. "ADMIN"
            .map(roleName -> "ROLE_" + roleName) 
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        // Build a userDetails object with the roles
        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())  // hashed password from DB
            .authorities(authorities)
            .build();
    }
}
