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

    /*@Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // 🔍 Debug: Print roles
        System.out.println("Roles loaded from database: " + user.getRoles());

        // Convert roles to Spring Security authorities
        var authorities = user.getRoles().stream()
            .map(role -> "ROLE_" + role.name())  // Ensure "ROLE_" prefix is added
            .map(SimpleGrantedAuthority::new)
            .toList();

        // 🔍 Debug: Print extracted authorities
        System.out.println("Spring Authorities: " + authorities);

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword())  
            .authorities(authorities)
            .build();
    }*/

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                .map(role -> "ROLE_" + role.name())
                .toArray(String[]::new))
                .build();
    }


}
