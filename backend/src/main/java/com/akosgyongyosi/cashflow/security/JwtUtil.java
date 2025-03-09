package com.akosgyongyosi.cashflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

@Component
public class JwtUtil {

    private final Key SECRET_KEY = Keys.hmacShaKeyFor(
        // Must be a valid base64-encoded string or use a random 256-bit key
        Decoders.BASE64.decode("8Abw2RxHm3uTmf9GsDXnf4leMVJA0u7sBw4VHrwlCyo=")
    );

    // 10 hours
    private final long EXPIRATION_TIME = 1000 * 60 * 60 * 10;

    public String generateToken(UserDetails userDetails) {
        List<String> roleNames = userDetails.getAuthorities().stream()
            .map(auth -> auth.getAuthority())  // Keep "ROLE_ADMIN"
            .toList();
    
        System.out.println("Roles added to JWT: " + roleNames); // Debug
    
        return buildToken(userDetails.getUsername(), roleNames);
    }
    
    
    public String generateToken(String email, Set<com.akosgyongyosi.cashflow.entity.Role> roles) {
        List<String> roleNames = roles.stream()
            .map(Enum::name)
            .collect(Collectors.toList());

        return buildToken(email, roleNames);
    }

    private String buildToken(String subject, List<String> roleNames) {
        long now = System.currentTimeMillis();

        Map<String, Object> claims = Map.of("roles", roleNames);

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(now + EXPIRATION_TIME))
            .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                   .setSigningKey(SECRET_KEY)
                   .build()
                   .parseClaimsJws(token)
                   .getBody();
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }
}
