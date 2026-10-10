package app.service;

import app.dto.FeedbackRequest;
import app.model.Complaint;
import app.model.Feedback;
import app.model.User;
import app.repository.ComplaintRepository;
import app.repository.FeedbackRepository;
import app.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FeedbackService {

    private static final Logger logger = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.admin.email:}")
    private String configuredAdminEmail;

    @Value("${spring.mail.username:campusfixproblem@gmail.com}")
    private String defaultSystemEmail;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.feedbackRepository = feedbackRepository;
        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public Feedback submitFeedback(Long complaintId, FeedbackRequest request, User authenticatedStudent) {
        if (complaintId == null) {
            throw new RuntimeException("Complaint ID is required.");
        }

        if (request == null) {
            throw new RuntimeException("Feedback request cannot be empty.");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Complaint not found with ID: " + complaintId));

        // SECURITY: Verify that the authenticated student owns this complaint
        if (complaint.getUser() == null || !complaint.getUser().getId().equals(authenticatedStudent.getId())) {
            throw new RuntimeException("You can only submit feedback for your own complaints.");
        }

        // Verify complaint is resolved or reopened
        String currentStatus = complaint.getStatus() != null ? complaint.getStatus().toUpperCase() : "";
        if (!"RESOLVED".equals(currentStatus) && !"REOPENED".equals(currentStatus)) {
            throw new RuntimeException("Feedback can only be submitted for resolved complaints.");
        }

        // Validate and normalize rating
        int rating = request.getRating();
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5 stars.");
        }

        // Validate issueFixed status
        final String issueFixed = (request.getIssueFixed() == null || request.getIssueFixed().trim().isEmpty())
                ? "Yes, completely fixed"
                : request.getIssueFixed().trim();

        boolean isNotResolved = "No, the issue still exists".equalsIgnoreCase(issueFixed);
        final boolean reopenRequested = isNotResolved && request.isReopenRequested();

        // 1. UPDATE COMPLAINT STATUS IF REOPEN REQUESTED
        if (reopenRequested) {
            complaint.setStatus("REOPENED");
            complaintRepository.save(complaint);
            logger.info("Complaint #{} status changed to REOPENED upon student request", complaint.getId());
        }

        // 2. SAVE FEEDBACK IN MYSQL (Must happen BEFORE email notification)
        Feedback feedback = feedbackRepository.findByComplaintId(complaint.getId())
                .orElseGet(() -> new Feedback(complaint, authenticatedStudent, rating, issueFixed, request.getComment(), reopenRequested));

        feedback.setRating(rating);
        feedback.setIssueFixed(issueFixed);
        feedback.setComment(request.getComment() != null ? request.getComment().trim() : "");
        feedback.setReopenRequested(reopenRequested);

        Feedback savedFeedback = feedbackRepository.save(feedback);
        logger.info("Feedback #{} successfully saved for Complaint #{}", savedFeedback.getId(), complaint.getId());

        // 3. SEND EMAIL NOTIFICATION TO ADMINISTRATOR
        // (Any email failure must be caught and logged without affecting the saved feedback)
        try {
            sendAdministratorNotification(complaint, authenticatedStudent, savedFeedback);
        } catch (Exception ex) {
            logger.error("Failed to send admin feedback email notification for Complaint #{}: {}", complaint.getId(), ex.getMessage(), ex);
        }

        return savedFeedback;
    }

    public Optional<Feedback> getFeedbackForComplaint(Long complaintId) {
        return feedbackRepository.findByComplaintId(complaintId);
    }

    private void sendAdministratorNotification(Complaint complaint, User student, Feedback feedback) {
        // Collect administrator emails from database
        Set<String> recipientEmails = new HashSet<>();

        List<User> adminUsers = userRepository.findByRoleAndAccountStatus("ADMIN", "ACTIVE");
        if (adminUsers != null && !adminUsers.isEmpty()) {
            for (User admin : adminUsers) {
                if (admin.getEmail() != null && !admin.getEmail().trim().isEmpty()) {
                    recipientEmails.add(admin.getEmail().trim().toLowerCase());
                }
            }
        }

        // Fallback to configured admin email or system email
        if (recipientEmails.isEmpty()) {
            if (configuredAdminEmail != null && !configuredAdminEmail.trim().isEmpty()) {
                recipientEmails.add(configuredAdminEmail.trim().toLowerCase());
            } else if (defaultSystemEmail != null && !defaultSystemEmail.trim().isEmpty()) {
                recipientEmails.add(defaultSystemEmail.trim().toLowerCase());
            }
        }

        if (recipientEmails.isEmpty()) {
            logger.warn("No administrator email found to receive feedback notification for Complaint #{}", complaint.getId());
            return;
        }

        // Determine email subject
        String subject;
        if (feedback.isReopenRequested()) {
            subject = "Fixora - Complaint Reopening Requested #" + complaint.getId();
        } else if (feedback.getRating() <= 2 || "Partially fixed".equalsIgnoreCase(feedback.getIssueFixed()) || "No, the issue still exists".equalsIgnoreCase(feedback.getIssueFixed())) {
            subject = "Fixora - Complaint Feedback Requires Attention #" + complaint.getId();
        } else {
            subject = "Fixora - Student Feedback Received for Complaint #" + complaint.getId();
        }

        // Format star rating
        StringBuilder starDisplay = new StringBuilder();
        for (int i = 0; i < feedback.getRating(); i++) {
            starDisplay.append("★");
        }
        for (int i = feedback.getRating(); i < 5; i++) {
            starDisplay.append("☆");
        }
        String ratingFormatted = feedback.getRating() + " stars (" + starDisplay + ")";

        // Format submission timestamp
        String formattedDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .format(feedback.getCreatedAt() != null ? feedback.getCreatedAt() : LocalDateTime.now());

        // Construct email body matching requested format
        StringBuilder body = new StringBuilder();
        body.append("Fixora - Student Feedback Notification\n\n");
        body.append("Complaint ID: ").append(complaint.getId()).append("\n");
        body.append("Complaint Title: ").append(complaint.getTitle() != null ? complaint.getTitle() : "N/A").append("\n");
        body.append("Category: ").append(complaint.getCategory() != null ? complaint.getCategory() : "GENERAL").append("\n");
        body.append("Student Name: ").append(student.getName() != null ? student.getName() : "Student").append("\n\n");

        body.append("Student Rating: ").append(ratingFormatted).append("\n\n");

        body.append("Was the issue fixed properly?\n");
        body.append(feedback.getIssueFixed()).append("\n\n");

        body.append("Student Comment:\n");
        body.append(feedback.getComment() != null && !feedback.getComment().trim().isEmpty() ? feedback.getComment() : "No comment provided").append("\n\n");

        body.append("Feedback Submitted:\n");
        body.append(formattedDateTime).append("\n\n");

        // Action required section if issue still exists
        if ("No, the issue still exists".equalsIgnoreCase(feedback.getIssueFixed())) {
            body.append("⚠️ ACTION REQUIRED\n\n");
            body.append("The student reported that the issue is still not resolved.\n\n");
            body.append("Reopen Requested:\n");
            body.append(feedback.isReopenRequested() ? "Yes" : "No").append("\n\n");
        }

        body.append("---\n");
        body.append("Fixora Automated Complaint Management System");

        String bodyContent = body.toString();

        // Send to all administrators
        for (String adminEmail : recipientEmails) {
            try {
                emailService.sendAdminFeedbackNotification(adminEmail, subject, bodyContent);
                logger.info("Feedback email notification sent to administrator: {} for Complaint #{}", adminEmail, complaint.getId());
            } catch (Exception ex) {
                logger.error("Error sending feedback email to {}: {}", adminEmail, ex.getMessage());
            }
        }
    }
}
