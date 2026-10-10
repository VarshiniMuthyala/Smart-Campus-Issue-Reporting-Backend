package app.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpiration;

    private Key signingKey;

    // =====================================================
    // INITIALIZE SECRET KEY
    // =====================================================

    @PostConstruct
    public void initialize() {

        if (secret == null
                || secret.trim().length() < 32) {

            throw new IllegalStateException(
                    "JWT secret must contain at least 32 characters."
            );
        }

        signingKey =
                Keys.hmacShaKeyFor(
                        secret.getBytes(
                                StandardCharsets.UTF_8
                        )
                );
    }

    // =====================================================
    // GENERATE TOKEN
    // =====================================================

    public String generateToken(
            UserDetails userDetails) {

        return generateToken(
                userDetails,
                extractRole(userDetails)
        );
    }

    // =====================================================
    // GENERATE TOKEN WITH ROLE
    // =====================================================

    public String generateToken(
            UserDetails userDetails,
            String role) {

        Map<String, Object> claims =
                new HashMap<>();

        claims.put(
                "role",
                role.toUpperCase()
        );

        return Jwts.builder()

                .setClaims(claims)

                .setSubject(
                        userDetails.getUsername()
                )

                .setIssuedAt(
                        new Date()
                )

                .setExpiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtExpiration
                        )
                )

                .signWith(
                        signingKey,
                        SignatureAlgorithm.HS256
                )

                .compact();
    }

    // =====================================================
    // EXTRACT USERNAME / EMAIL
    // =====================================================

    public String extractUsername(
            String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }

    // =====================================================
    // EXTRACT ROLE
    // =====================================================

    public String extractRole(
            String token) {

        Claims claims =
                extractAllClaims(token);

        return claims.get(
                "role",
                String.class
        );
    }

    // =====================================================
    // EXTRACT EXPIRATION
    // =====================================================

    public Date extractExpiration(
            String token) {

        return extractClaim(
                token,
                Claims::getExpiration
        );
    }

    // =====================================================
    // GENERIC CLAIM EXTRACTION
    // =====================================================

    public <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver) {

        Claims claims =
                extractAllClaims(token);

        return claimsResolver.apply(
                claims
        );
    }

    // =====================================================
    // READ ALL CLAIMS
    // =====================================================

    private Claims extractAllClaims(
            String token) {

        return Jwts.parserBuilder()

                .setSigningKey(
                        signingKey
                )

                .build()

                .parseClaimsJws(
                        token
                )

                .getBody();
    }

    // =====================================================
    // CHECK EXPIRATION
    // =====================================================

    private boolean isTokenExpired(
            String token) {

        return extractExpiration(
                token
        ).before(
                new Date()
        );
    }

    // =====================================================
    // VALIDATE TOKEN
    // =====================================================

    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        try {

            String username =
                    extractUsername(token);

            return username.equals(
                    userDetails.getUsername()
            )
                    && !isTokenExpired(token);

        } catch (Exception e) {

            return false;
        }
    }

    // =====================================================
    // EXTRACT ROLE FROM USER DETAILS
    // =====================================================

    private String extractRole(
            UserDetails userDetails) {

        if (userDetails
                .getAuthorities()
                .isEmpty()) {

            throw new IllegalStateException(
                    "User role is missing."
            );
        }

        String authority =
                userDetails
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority();

        if (!authority.startsWith("ROLE_")) {

            throw new IllegalStateException(
                    "Invalid user role."
            );
        }

        return authority.substring(
                "ROLE_".length()
        );
    }
}