package app.service;

import io.jsonwebtoken.ExpiredJwtException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService =
                userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader("Authorization");

        // =================================================
        // NO TOKEN
        // =================================================

        if (authorizationHeader == null
                || !authorizationHeader
                        .startsWith("Bearer ")) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authorizationHeader.substring(7);

        try {

            // =============================================
            // EXTRACT EMAIL
            // =============================================

            String email =
                    jwtService.extractUsername(token);

            // =============================================
            // ONLY AUTHENTICATE IF NO EXISTING AUTH
            // =============================================

            if (email != null
                    && SecurityContextHolder
                    .getContext()
                    .getAuthentication() == null) {

                // =========================================
                // LOAD USER
                // =========================================

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        email
                                );

                // =========================================
                // VALIDATE JWT
                // =========================================

                if (jwtService.isTokenValid(
                        token,
                        userDetails
                )) {

                    String role =
                            jwtService.extractRole(
                                    token
                            );

                    if (role == null
                            || role.trim().isEmpty()) {

                        role =
                                extractRoleFromUser(
                                        userDetails
                                );
                    }

                    String authority =
                            "ROLE_"
                                    + role
                                    .trim()
                                    .toUpperCase();

                    UsernamePasswordAuthenticationToken
                            authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    Collections.singletonList(
                                            new SimpleGrantedAuthority(
                                                    authority
                                            )
                                    )
                            );

                    authentication.setDetails(
                            request
                    );

                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authentication
                            );
                }
            }

        } catch (ExpiredJwtException e) {

            // Token expired.
            // Continue without authentication.
            SecurityContextHolder
                    .clearContext();

        } catch (Exception e) {

            // Invalid token.
            // Do not allow it to authenticate the user.
            SecurityContextHolder
                    .clearContext();
        }

        filterChain.doFilter(
                request,
                response
        );
    }

    // =====================================================
    // FALLBACK ROLE
    // =====================================================

    private String extractRoleFromUser(
            UserDetails userDetails) {

        if (userDetails
                .getAuthorities()
                .isEmpty()) {

            throw new RuntimeException(
                    "User role not found."
            );
        }

        String authority =
                userDetails
                        .getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority();

        if (!authority.startsWith("ROLE_")) {

            throw new RuntimeException(
                    "Invalid user role."
            );
        }

        return authority.substring(5);
    }
}