package app.controller;

import app.dto.ForgotPasswordRequest;
import app.dto.LoginRequest;
import app.dto.ResetPasswordRequest;
import app.dto.SignupRequest;
import app.dto.UserResponse;
import app.model.User;
import app.service.AuthService;
import app.service.CustomUserDetailsService;
import app.service.JwtService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            CustomUserDetailsService customUserDetailsService) {

        this.authService = authService;
        this.jwtService = jwtService;
        this.customUserDetailsService =
                customUserDetailsService;
    }

    // =====================================================
    // SIGNUP
    // =====================================================

    @PostMapping("/signup")
    public ResponseEntity<?> signup(
            @RequestBody SignupRequest request) {

        try {

            User user =
                    authService.signup(request);

            return ResponseEntity.ok(
                    new UserResponse(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getRole()
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            User user =
                    authService.login(request);

            // -------------------------------------------------
            // LOAD USER FOR SPRING SECURITY
            // -------------------------------------------------

            UserDetails userDetails =
                    customUserDetailsService
                            .loadUserByUsername(
                                    user.getEmail()
                            );

            // -------------------------------------------------
            // GENERATE JWT
            // -------------------------------------------------

            String token =
                    jwtService.generateToken(
                            userDetails,
                            user.getRole()
                    );

            // -------------------------------------------------
            // RETURN USER + TOKEN
            // -------------------------------------------------

            return ResponseEntity.ok(
                    new UserResponse(
                            user.getId(),
                            user.getName(),
                            user.getEmail(),
                            user.getRole(),
                            token
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // FORGOT PASSWORD
    // =====================================================

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {

        try {

            authService.forgotPassword(request);

            // Do not reveal whether email exists for security
            return ResponseEntity.ok(
                    Collections.singletonMap(
                            "message",
                            "If the email address is registered, we will send you a password reset link."
                    )
            );

        } catch (Exception e) {

            return ResponseEntity.ok(
                    Collections.singletonMap(
                            "message",
                            "If the email address is registered, we will send you a password reset link."
                    )
            );
        }
    }

    // =====================================================
    // VERIFY RESET TOKEN
    // =====================================================

    @GetMapping("/verify-reset-token")
    public ResponseEntity<?> verifyResetToken(
            @RequestParam String token) {

        boolean valid = authService.verifyResetToken(token);

        return ResponseEntity.ok(
                Collections.singletonMap("valid", valid)
        );
    }

    // =====================================================
    // RESET PASSWORD
    // =====================================================

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody ResetPasswordRequest request) {

        try {

            authService.resetPassword(request);

            return ResponseEntity.ok(
                    Collections.singletonMap(
                            "message",
                            "Password reset successfully. You can now log in with your new password."
                    )
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("message", e.getMessage()));

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("message", "Unable to reset password. Please try again."));
        }
    }
}