package com.akosgyongyosi.cashflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.akosgyongyosi.cashflow.entity.Role;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtUtil {

    private static final Key secretKey = Keys.hmacShaKeyFor(
        Decoders.BASE64.decode("8Abw2RxHm3uTmf9GsDXnf4leMVJA0u7sBw4VHrwlCyo=")
    );
    private static final long EXPIRATION_TIME_MS = 1000L * 60 * 60 * 10; // 10 hours

    public String generateToken(UserDetails userDetails) {
        List<String> roleNames = userDetails.getAuthorities().stream()
            .map(auth -> auth.getAuthority())
            .toList();

        return buildToken(userDetails.getUsername(), roleNames);
    }

    public String generateToken(String email, Set<Role> roles) {
        // Ensure consistency with Spring Security's ROLE_ prefix expectations
        // so that both hasRole("ADMIN") and hasAuthority("ROLE_ADMIN") checks succeed.
        List<String> roleNames = roles.stream()
            .map(r -> "ROLE_" + r.name())
            .toList();
        return buildToken(email, roleNames);
    }

    private String buildToken(String subject, List<String> roleNames) {
        long now = System.currentTimeMillis();

        Map<String, Object> claims = Map.of("roles", roleNames);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + EXPIRATION_TIME_MS))
        .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
        .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
