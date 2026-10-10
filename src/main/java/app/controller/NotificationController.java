package app.controller;

import app.model.Notification;
import app.model.User;
import app.repository.UserRepository;
import app.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(
            NotificationService notificationService,
            UserRepository userRepository) {

        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    // =====================================================
    // GET MY NOTIFICATIONS
    // =====================================================

    @GetMapping
    public ResponseEntity<?> getNotifications(
            Authentication authentication) {

        try {

            User user =
                    getAuthenticatedUser(authentication);

            List<Notification> notifications =
                    notificationService.getUserNotifications(
                            user.getId()
                    );

            return ResponseEntity.ok(notifications);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // GET MY UNREAD NOTIFICATIONS
    // =====================================================

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            Authentication authentication) {

        try {

            User user =
                    getAuthenticatedUser(authentication);

            List<Notification> notifications =
                    notificationService.getUnreadNotifications(
                            user.getId()
                    );

            return ResponseEntity.ok(notifications);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // MARK MY NOTIFICATION AS READ
    // =====================================================

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            User user =
                    getAuthenticatedUser(authentication);

            Notification notification =
                    notificationService.markAsRead(
                            id,
                            user.getId()
                    );

            return ResponseEntity.ok(notification);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =====================================================
    // AUTHENTICATED USER
    // =====================================================

    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new RuntimeException(
                    "Authentication is required."
            );
        }

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found."
                        )
                );
    }
}