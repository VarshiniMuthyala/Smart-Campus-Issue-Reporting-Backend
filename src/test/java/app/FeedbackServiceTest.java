package app;

import app.dto.FeedbackRequest;
import app.model.Complaint;
import app.model.Feedback;
import app.model.User;
import app.repository.ComplaintRepository;
import app.repository.FeedbackRepository;
import app.repository.UserRepository;
import app.service.EmailService;
import app.service.FeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FeedbackServiceTest {

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @MockBean
    private EmailService emailService;

    private User testStudent;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();

        // Setup test student
        testStudent = userRepository.findByEmail("student.test.feedback@fixora.com").orElseGet(() -> {
            User student = new User();
            student.setName("Test Student");
            student.setEmail("student.test.feedback@fixora.com");
            student.setPassword("hashedpassword123");
            student.setRole("STUDENT");
            student.setAccountStatus("ACTIVE");
            return userRepository.save(student);
        });

        // Setup test admin
        testAdmin = userRepository.findByEmail("admin.test.feedback@fixora.com").orElseGet(() -> {
            User admin = new User();
            admin.setName("Test Admin");
            admin.setEmail("admin.test.feedback@fixora.com");
            admin.setPassword("hashedpassword123");
            admin.setRole("ADMIN");
            admin.setAccountStatus("ACTIVE");
            return userRepository.save(admin);
        });
    }

    private Complaint createResolvedComplaint(String title) {
        Complaint complaint = new Complaint();
        complaint.setTitle(title);
        complaint.setDescription("Test description for feedback test");
        complaint.setLocation("Building A - Lab 1");
        complaint.setCategory("ELECTRICAL");
        complaint.setPriority("MEDIUM");
        complaint.setStatus("RESOLVED");
        complaint.setUser(testStudent);
        return complaintRepository.save(complaint);
    }

    @Test
    void testScenario1_FiveStarNormalFeedback_SendsNormalEmail() {
        // Arrange
        Complaint complaint = createResolvedComplaint("AC not cooling properly");
        FeedbackRequest request = new FeedbackRequest(5, "Yes, completely fixed", "Great job, works perfectly now!", false);

        // Act
        Feedback feedback = feedbackService.submitFeedback(complaint.getId(), request, testStudent);

        // Assert feedback saved in database
        assertNotNull(feedback.getId());
        assertEquals(5, feedback.getRating());
        assertEquals("Yes, completely fixed", feedback.getIssueFixed());
        assertFalse(feedback.isReopenRequested());

        // Verify complaint status remains RESOLVED
        Complaint reloaded = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals("RESOLVED", reloaded.getStatus());

        // Verify email sent with normal feedback subject
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendAdminFeedbackNotification(anyString(), subjectCaptor.capture(), bodyCaptor.capture());

        String capturedSubject = subjectCaptor.getValue();
        String capturedBody = bodyCaptor.getValue();

        assertTrue(capturedSubject.contains("Fixora - Student Feedback Received for Complaint #" + complaint.getId()));
        assertTrue(capturedBody.contains("Complaint ID: " + complaint.getId()));
        assertTrue(capturedBody.contains("Student Rating: 5 stars (★★★★★)"));
        assertTrue(capturedBody.contains("Yes, completely fixed"));
        assertFalse(capturedBody.contains("⚠️ ACTION REQUIRED"));
    }

    @Test
    void testScenario2_TwoStarPartiallyFixedFeedback_SendsAttentionEmail() {
        // Arrange
        Complaint complaint = createResolvedComplaint("Projector HDMI flickering");
        FeedbackRequest request = new FeedbackRequest(2, "Partially fixed", "Display works now but color is slightly distorted", false);

        // Act
        Feedback feedback = feedbackService.submitFeedback(complaint.getId(), request, testStudent);

        // Assert feedback saved
        assertNotNull(feedback.getId());
        assertEquals(2, feedback.getRating());
        assertEquals("Partially fixed", feedback.getIssueFixed());
        assertFalse(feedback.isReopenRequested());

        // Verify email subject is "Fixora - Complaint Feedback Requires Attention"
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendAdminFeedbackNotification(anyString(), subjectCaptor.capture(), bodyCaptor.capture());

        String capturedSubject = subjectCaptor.getValue();
        String capturedBody = bodyCaptor.getValue();

        assertTrue(capturedSubject.contains("Fixora - Complaint Feedback Requires Attention #" + complaint.getId()));
        assertTrue(capturedBody.contains("Complaint ID: " + complaint.getId()));
        assertTrue(capturedBody.contains("Student Rating: 2 stars (★★☆☆☆)"));
        assertTrue(capturedBody.contains("Partially fixed"));
    }

    @Test
    void testScenario3_OneStarIssueStillExistsWithReopen_ReopensComplaintAndSendsActionRequiredEmail() {
        // Arrange
        Complaint complaint = createResolvedComplaint("Water leakage in ceiling");
        FeedbackRequest request = new FeedbackRequest(1, "No, the issue still exists", "Water is still dripping after technician left", true);

        // Act
        Feedback feedback = feedbackService.submitFeedback(complaint.getId(), request, testStudent);

        // Assert feedback saved with reopenRequested = true
        assertNotNull(feedback.getId());
        assertEquals(1, feedback.getRating());
        assertEquals("No, the issue still exists", feedback.getIssueFixed());
        assertTrue(feedback.isReopenRequested());

        // Verify complaint status changed to REOPENED in MySQL!
        Complaint reloaded = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals("REOPENED", reloaded.getStatus());

        // Verify email sent with REOPENING and ACTION REQUIRED markers
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendAdminFeedbackNotification(anyString(), subjectCaptor.capture(), bodyCaptor.capture());

        String capturedSubject = subjectCaptor.getValue();
        String capturedBody = bodyCaptor.getValue();

        assertTrue(capturedSubject.contains("Fixora - Complaint Reopening Requested #" + complaint.getId()));
        assertTrue(capturedBody.contains("⚠️ ACTION REQUIRED"));
        assertTrue(capturedBody.contains("The student reported that the issue is still not resolved."));
        assertTrue(capturedBody.contains("Reopen Requested:\nYes"));
    }
}
