package app.controller;

import app.dto.ComplaintRequest;
import app.model.Complaint;
import app.model.User;
import app.repository.UserRepository;
import app.service.ComplaintService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import app.dto.FeedbackRequest;
import app.model.Feedback;
import app.service.FeedbackService;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:5173")
public class ComplaintController {

    private final ComplaintService complaintService;
    private final UserRepository userRepository;
    private final FeedbackService feedbackService;

    public ComplaintController(
            ComplaintService complaintService,
            UserRepository userRepository,
            FeedbackService feedbackService) {

        this.complaintService = complaintService;
        this.userRepository = userRepository;
        this.feedbackService = feedbackService;
    }

    // =========================================================
    // CREATE COMPLAINT
    // STUDENT ONLY
    // =========================================================

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createComplaint(

            @RequestParam String title,

            @RequestParam String description,

            @RequestParam String location,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            String priority,

            @RequestParam(value = "image", required = false)
            MultipartFile image,

            @RequestParam(value = "images", required = false)
            List<MultipartFile> images,

            Authentication authentication) {

        try {

            // -------------------------------------------------
            // GET LOGGED-IN USER
            // -------------------------------------------------

            User user =
                    getAuthenticatedUser(authentication);

            // -------------------------------------------------
            // CREATE REQUEST OBJECT
            // -------------------------------------------------

            ComplaintRequest request =
                    new ComplaintRequest();

            request.setTitle(title);

            request.setDescription(
                    description
            );

            request.setLocation(
                    location
            );

            request.setCategory(
                    category
            );

            request.setPriority(
                    priority
            );

            // -------------------------------------------------
            // COLLECT IMAGES (Support both "image" and "images")
            // -------------------------------------------------

            List<MultipartFile> fileList = new java.util.ArrayList<>();
            if (images != null) {
                fileList.addAll(images);
            }
            if (image != null && !image.isEmpty()) {
                fileList.add(image);
            }

            // -------------------------------------------------
            // CREATE COMPLAINT
            // -------------------------------------------------

            Complaint complaint =
                    complaintService.createComplaint(
                            request,
                            user.getId(),
                            fileList
                    );

            return ResponseEntity.ok(
                    complaint
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Unable to create complaint: "
                                    + e.getMessage()
                    );
        }
    }

    // =========================================================
    // GET MY COMPLAINTS
    // STUDENT ONLY
    // =========================================================

    @GetMapping("/my")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyComplaints(
            Authentication authentication) {

        try {

            User user =
                    getAuthenticatedUser(
                            authentication
                    );

            List<Complaint> complaints =
                    complaintService.getUserComplaints(
                            user.getId()
                    );

            return ResponseEntity.ok(
                    complaints
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Unable to fetch complaints: "
                                    + e.getMessage()
                    );
        }
    }

    // =========================================================
    // GET COMPLAINT BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getComplaint(
            @PathVariable Long id,
            Authentication authentication) {

        try {

            User user =
                    getAuthenticatedUser(
                            authentication
                    );

            Complaint complaint =
                    complaintService.getComplaint(id);

            // -------------------------------------------------
            // ADMIN CAN VIEW ANY COMPLAINT
            // -------------------------------------------------

            if ("ADMIN".equalsIgnoreCase(
                    user.getRole())) {

                return ResponseEntity.ok(
                        complaint
                );
            }

            // -------------------------------------------------
            // TECHNICIAN CAN VIEW ASSIGNED COMPLAINT
            // -------------------------------------------------

            if ("TECHNICIAN".equalsIgnoreCase(
                    user.getRole())) {

                if (complaint.getTechnician() != null
                        && complaint
                                .getTechnician()
                                .getEmail()
                                .equalsIgnoreCase(
                                        user.getEmail()
                                )) {

                    return ResponseEntity.ok(
                            complaint
                    );
                }

                return ResponseEntity
                        .status(403)
                        .body(
                                "You are not assigned to this complaint"
                        );
            }

            // -------------------------------------------------
            // STUDENT CAN VIEW OWN COMPLAINT
            // -------------------------------------------------

            if (complaint.getUser() != null
                    && complaint
                            .getUser()
                            .getId()
                            .equals(user.getId())) {

                return ResponseEntity.ok(
                        complaint
                );
            }

            return ResponseEntity
                    .status(403)
                    .body(
                            "You are not allowed to view this complaint"
                    );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Unable to fetch complaint: "
                                    + e.getMessage()
                    );
        }
    }

    // =========================================================
    // EDIT COMPLAINT
    // STUDENT ONLY
    //
    // Student can edit only their own PENDING complaint.
    // Existing complaint image is kept.
    // =========================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateComplaint(

            @PathVariable Long id,

            @RequestParam String title,

            @RequestParam String description,

            @RequestParam String location,

            @RequestParam(required = false)
            String category,

            @RequestParam(required = false)
            String priority,

            @RequestParam(value = "image", required = false)
            MultipartFile image,

            @RequestParam(value = "images", required = false)
            List<MultipartFile> images,

            Authentication authentication) {

        try {

            // -------------------------------------------------
            // GET LOGGED-IN USER
            // -------------------------------------------------

            User user =
                    getAuthenticatedUser(
                            authentication
                    );

            // -------------------------------------------------
            // CREATE UPDATED REQUEST
            // -------------------------------------------------

            ComplaintRequest request =
                    new ComplaintRequest();

            request.setTitle(title);

            request.setDescription(
                    description
            );

            request.setLocation(
                    location
            );

            request.setCategory(
                    category
            );

            request.setPriority(
                    priority
            );

            // -------------------------------------------------
            // COLLECT IMAGES
            // -------------------------------------------------

            List<MultipartFile> fileList = new java.util.ArrayList<>();
            if (images != null) {
                fileList.addAll(images);
            }
            if (image != null && !image.isEmpty()) {
                fileList.add(image);
            }

            // -------------------------------------------------
            // UPDATE COMPLAINT
            // -------------------------------------------------

            Complaint updatedComplaint =
                    complaintService.updateComplaint(
                            id,
                            user.getId(),
                            request,
                            fileList.isEmpty() ? null : fileList
                    );

            return ResponseEntity.ok(
                    updatedComplaint
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Unable to update complaint: "
                                    + e.getMessage()
                    );
        }
    }

    // =========================================================
    // DELETE COMPLAINT
    // ADMIN ONLY
    // =========================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteComplaint(
            @PathVariable Long id) {

        try {

            complaintService.deleteComplaint(id);

            return ResponseEntity.ok(
                    "Complaint deleted successfully"
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Unable to delete complaint: "
                                    + e.getMessage()
                    );
        }
    }

    // =========================================================
    // GET AUTHENTICATED USER
    // =========================================================

    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName()
                        .trim()
                        .isEmpty()) {

            throw new RuntimeException(
                    "User is not authenticated"
            );
        }

        return userRepository
                .findByEmail(
                        authentication
                                .getName()
                                .trim()
                                .toLowerCase()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found"
                        )
                );
    }

    // =========================================================
    // STUDENT FEEDBACK
    // =========================================================

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitFeedback(
            @PathVariable Long id,
            @RequestBody FeedbackRequest request,
            Authentication authentication) {

        try {
            User student = getAuthenticatedUser(authentication);
            feedbackService.submitFeedback(id, request, student);

            return ResponseEntity.ok(
                    java.util.Collections.singletonMap(
                            "message",
                            "Thank you! Your feedback has been submitted successfully."
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    java.util.Collections.singletonMap("message", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    java.util.Collections.singletonMap("message", "Failed to submit feedback. Please try again.")
            );
        }
    }

    @GetMapping("/{id}/feedback")
    public ResponseEntity<?> getFeedback(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            return feedbackService.getFeedbackForComplaint(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.noContent().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    java.util.Collections.singletonMap("message", e.getMessage())
            );
        }
    }
}