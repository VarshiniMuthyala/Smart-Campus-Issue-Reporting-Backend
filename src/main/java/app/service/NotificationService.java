package app.service;

import app.model.Notification;
import app.model.User;
import app.repository.NotificationRepository;
import app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {

        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CREATE NOTIFICATION USING USER ID
    // =========================================================

    public Notification createNotification(
            Long userId,
            String message,
            String type) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        return createNotification(
                user,
                message,
                type
        );
    }

    // =========================================================
    // CREATE NOTIFICATION USING USER OBJECT
    // =========================================================

    public Notification createNotification(
            User user,
            String message,
            String type) {

        if (user == null) {
            throw new RuntimeException(
                    "Cannot create notification for null user"
            );
        }

        Notification notification =
                new Notification();

        notification.setUser(user);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);

        return notificationRepository.save(
                notification
        );
    }

    // =========================================================
    // GET ALL USER NOTIFICATIONS
    // =========================================================

    public List<Notification> getUserNotifications(
            Long userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user);
    }

    // =========================================================
    // GET UNREAD NOTIFICATIONS
    // =========================================================

    public List<Notification> getUnreadNotifications(
            Long userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        return notificationRepository
                .findByUserAndReadFalseOrderByCreatedAtDesc(
                        user
                );
    }

    // =========================================================
    // MARK NOTIFICATION AS READ
    // =========================================================

    public Notification markAsRead(
            Long notificationId,
            Long userId) {

        Notification notification =
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        if (notification.getUser() == null
                || notification.getUser().getId() == null
                || !notification.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You cannot modify another user's notification"
            );
        }

        notification.setRead(true);

        return notificationRepository.save(
                notification
        );
    }
}