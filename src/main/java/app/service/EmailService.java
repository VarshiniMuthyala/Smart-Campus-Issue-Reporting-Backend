package app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailService {

    private final Optional<JavaMailSender> mailSender;
    private final boolean enabled;

    public EmailService(
            Optional<JavaMailSender> mailSender,
            @Value("${app.email.enabled:false}") boolean enabled) {

        this.mailSender = mailSender;
        this.enabled = enabled;
    }

    public void sendEmail(String to, String subject, String message) {

        if (!enabled) {
            System.out.println("CampusFix email notifications are disabled.");
            return;
        }

        if (to == null || to.trim().isEmpty()) {
            return;
        }

        if (mailSender.isEmpty()) {
            System.err.println(
                    "CampusFix email is enabled, but JavaMailSender is not configured."
            );
            return;
        }

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(to.trim());
            mail.setSubject(subject == null ? "CampusFix Notification" : subject);
            mail.setText(message == null ? "" : message);
            mailSender.get().send(mail);
        } catch (Exception ex) {
            // Email failure must not break complaint/user operations.
            System.err.println(
                    "CampusFix email could not be sent to " + to + ": " + ex.getMessage()
            );
        }
    }

    public void sendAdminFeedbackNotification(String adminEmail, String subject, String message) {
        sendEmail(adminEmail, subject, message);
    }
}
