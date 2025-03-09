package com.akosgyongyosi.cashflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(authenticationManager -> null);
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws IOException, ServletException {

        String token = getJwtFromRequest(request);

        if (token != null) {
            try {
                // 1) Parse once
                Claims claims = jwtUtil.parseToken(token);

                // 2) Check expiry
                if (!jwtUtil.isTokenExpired(claims)) {
                    String email = claims.getSubject();

                    // roles stored as ["ADMIN","VIEWER"]
                    @SuppressWarnings("unchecked")
                    List<String> roleNames = claims.get("roles", List.class);

                    // Convert each string role name into a GrantedAuthority
                    List<GrantedAuthority> authorities = roleNames.stream()
                        // e.g. "ADMIN" => new SimpleGrantedAuthority("ROLE_ADMIN")
                        .map(roleName -> new SimpleGrantedAuthority("ROLE_" + roleName))
                        .collect(Collectors.toList());

                    // Build the Spring Security UserDetails
                    User userDetails = new User(email, "", authorities);

                    // Create authentication token
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (JwtException e) {
                // Malformed or invalid token
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid or expired token");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
