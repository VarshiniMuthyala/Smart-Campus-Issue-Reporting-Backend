package app.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordHash {

    private final PasswordEncoder passwordEncoder;

    public PasswordHash() {
        this.passwordEncoder =
                new BCryptPasswordEncoder();
    }

    // =====================================================
    // ENCODE PASSWORD
    // =====================================================

    public String encode(String password) {

        if (password == null
                || password.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Password cannot be empty."
            );
        }

        return passwordEncoder.encode(
                password
        );
    }

    // =====================================================
    // CHECK PASSWORD
    // =====================================================

    public boolean matches(
            String rawPassword,
            String encodedPassword) {

        if (rawPassword == null
                || encodedPassword == null) {

            return false;
        }

        // -------------------------------------------------
        // NEW BCrypt PASSWORD
        // -------------------------------------------------

        if (isBCryptHash(encodedPassword)) {

            return passwordEncoder.matches(
                    rawPassword,
                    encodedPassword
            );
        }

        // -------------------------------------------------
        // LEGACY PASSWORD
        // -------------------------------------------------

        return manualHash(
                rawPassword
        ).equals(
                encodedPassword
        );
    }

    // =====================================================
    // CHECK BCrypt HASH
    // =====================================================

    public boolean isBCryptHash(
            String passwordHash) {

        if (passwordHash == null) {
            return false;
        }

        return passwordHash.startsWith("$2a$")
                || passwordHash.startsWith("$2b$")
                || passwordHash.startsWith("$2y$");
    }

    // =====================================================
    // LEGACY HASH
    // =====================================================

    public String manualHash(
            String password) {

        if (password == null) {
            return "";
        }

        long hashValue = 7;

        final long PRIME = 31;
        final long MODULUS = 1_000_000_007L;

        for (char c :
                password.toCharArray()) {

            hashValue =
                    (hashValue * PRIME + c)
                            % MODULUS;
        }

        return Long.toString(
                hashValue
        );
    }

    // =====================================================
    // PASSWORD ENCODER
    // =====================================================

    public PasswordEncoder getPasswordEncoder() {

        return passwordEncoder;
    }
}