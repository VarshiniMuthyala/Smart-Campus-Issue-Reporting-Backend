package app.service;

import app.model.User;
import app.repository.UserRepository;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {

        this.userRepository =
                userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String email)
            throws UsernameNotFoundException {

        if (email == null
                || email.trim().isEmpty()) {

            throw new UsernameNotFoundException(
                    "Email cannot be empty."
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase();

        User user =
                userRepository
                        .findByEmail(normalizedEmail)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "User not found with email: "
                                                + normalizedEmail
                                )
                        );

        // =================================================
        // ACCOUNT STATUS
        // =================================================

        if (!"ACTIVE".equalsIgnoreCase(
                user.getAccountStatus()
        )) {

            throw new UsernameNotFoundException(
                    "User account is not active."
            );
        }

        // =================================================
        // ROLE
        // =================================================

        String role =
                user.getRole();

        if (role == null
                || role.trim().isEmpty()) {

            throw new UsernameNotFoundException(
                    "User role is not configured."
            );
        }

        String authority =
                "ROLE_"
                        + role.trim()
                        .toUpperCase();

        // =================================================
        // SPRING SECURITY USER
        // =================================================

        return new org.springframework.security
                .core.userdetails.User(
                        user.getEmail(),
                        user.getPassword(),
                        Collections.singletonList(
                                new SimpleGrantedAuthority(
                                        authority
                                )
                        )
                );
    }
}