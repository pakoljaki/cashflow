package com.akosgyongyosi.cashflow.security;

import com.akosgyongyosi.cashflow.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilRoleConsistencyTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void generatedTokenContainsPrefixedRoles() {
        String token = jwtUtil.generateToken("test@example.com", Set.of(Role.ADMIN, Role.USER));
        var claims = jwtUtil.parseToken(token);
        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) claims.get("roles");
        assertTrue(roles.contains("ROLE_ADMIN"), "ROLE_ADMIN should be present in token claims");
        assertTrue(roles.contains("ROLE_USER"), "ROLE_USER should be present in token claims");
        assertFalse(roles.contains("ADMIN"), "Unprefixed ADMIN should not be present");
    }

    @Test
    void tokenAuthoritiesRestoredMatchSpringExpectations() {
        String token = jwtUtil.generateToken("user@example.com", Set.of(Role.USER));
        var claims = jwtUtil.parseToken(token);
        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) claims.get("roles");
        var authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
        assertTrue(authorities.contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void generateTokenFromUserDetailsKeepsPrefixedAuthorities() {
        var user = new User("a@b.com", "pw", java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        var token = jwtUtil.generateToken(user);
        var claims = jwtUtil.parseToken(token);
        @SuppressWarnings("unchecked")
        var roles = (java.util.List<String>) claims.get("roles");
        assertEquals(1, roles.size());
        assertEquals("ROLE_ADMIN", roles.get(0));
    }
}
