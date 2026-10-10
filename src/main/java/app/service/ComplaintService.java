package app.service;

import app.dto.ComplaintRequest;
import app.model.Complaint;
import app.model.Technician;
import app.model.User;
import app.repository.ComplaintRepository;
import app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final AIComplaintService aiComplaintService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AutomaticAssignmentService automaticAssignmentService;

    public ComplaintService(
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            AIComplaintService aiComplaintService,
            FileStorageService fileStorageService,
            NotificationService notificationService,
            EmailService emailService,
            AutomaticAssignmentService automaticAssignmentService) {

        this.complaintRepository = complaintRepository;
        this.userRepository = userRepository;
        this.aiComplaintService = aiComplaintService;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.automaticAssignmentService = automaticAssignmentService;
    }

    // =========================================================
    // CREATE COMPLAINT
    // =========================================================

    public Complaint createComplaint(
            ComplaintRequest request,
            Long userId,
            MultipartFile image) {

        List<MultipartFile> list = image != null && !image.isEmpty()
                ? java.util.Collections.singletonList(image)
                : java.util.Collections.emptyList();

        return createComplaint(request, userId, list);
    }

    public Complaint createComplaint(
            ComplaintRequest request,
            Long userId,
            List<MultipartFile> images) {

        if (request == null) {
            throw new RuntimeException(
                    "Complaint request cannot be empty"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "User not found"
                        )
                );

        // -----------------------------------------------------
        // VALIDATION
        // -----------------------------------------------------

        if (request.getTitle() == null
                || request.getTitle().trim().isEmpty()) {

            throw new RuntimeException(
                    "Complaint title is required"
            );
        }

        if (request.getDescription() == null
                || request.getDescription().trim().isEmpty()) {

            throw new RuntimeException(
                    "Complaint description is required"
            );
        }

        if (request.getLocation() == null
                || request.getLocation().trim().isEmpty()) {

            throw new RuntimeException(
                    "Complaint location is required"
            );
        }

        // -----------------------------------------------------
        // PHOTO/PROOF IS COMPULSORY
        // -----------------------------------------------------

        if (images == null || images.isEmpty()) {

            throw new RuntimeException(
                    "Complaint photo or proof is required"
            );
        }

        // -----------------------------------------------------
        // SAVE IMAGES
        // -----------------------------------------------------

        String imageUrl =
                fileStorageService.saveImages(images);

        // -----------------------------------------------------
        // CREATE COMPLAINT
        // -----------------------------------------------------

        Complaint complaint = new Complaint();

        complaint.setTitle(
                request.getTitle().trim()
        );

        complaint.setDescription(
                request.getDescription().trim()
        );

        complaint.setLocation(
                request.getLocation().trim()
        );

        complaint.setUser(user);

        // -----------------------------------------------------
        // CATEGORY
        // -----------------------------------------------------

        String category = request.getCategory();

        if (category == null
                || category.trim().isEmpty()) {

            category =
                    aiComplaintService.categorizeComplaint(
                            request.getTitle(),
                            request.getDescription()
                    );
        }

        if (category == null
                || category.trim().isEmpty()) {

            category = "GENERAL";
        }

        complaint.setCategory(
                category.trim().toUpperCase()
        );

        // -----------------------------------------------------
        // PRIORITY
        // -----------------------------------------------------

        String priority = request.getPriority();

        if (priority == null
                || priority.trim().isEmpty()) {

            priority =
                    aiComplaintService.determinePriority(
                            request.getTitle(),
                            request.getDescription()
                    );
        }

        if (priority == null
                || priority.trim().isEmpty()) {

            priority = "MEDIUM";
        }

        complaint.setPriority(
                priority.trim().toUpperCase()
        );

        // -----------------------------------------------------
        // IMAGE
        // -----------------------------------------------------

        complaint.setImageUrl(imageUrl);

        // -----------------------------------------------------
        // DEFAULT STATUS
        // -----------------------------------------------------

        complaint.setStatus("PENDING");

        // -----------------------------------------------------
        // AUTOMATIC TECHNICIAN ASSIGNMENT
        // -----------------------------------------------------

        Technician assignedTechnician =
                automaticAssignmentService.findBestTechnician(
                        complaint
                );

        if (assignedTechnician != null) {

            complaint.setTechnician(
                    assignedTechnician
            );
        }

        // -----------------------------------------------------
        // SAVE
        // -----------------------------------------------------

        Complaint savedComplaint =
                complaintRepository.save(complaint);

        // -----------------------------------------------------
        // NOTIFY ACTIVE ADMINS
        // -----------------------------------------------------

        List<User> activeAdmins =
                userRepository.findByRoleAndAccountStatus(
                        "ADMIN",
                        "ACTIVE"
                );

        for (User admin : activeAdmins) {

            String notificationMessage;

            if (assignedTechnician != null) {

                notificationMessage =
                        "New complaint submitted and automatically assigned to "
                                + assignedTechnician.getName()
                                + ": "
                                + savedComplaint.getTitle();

            } else {

                notificationMessage =
                        "New complaint submitted but no available technician was found: "
                                + savedComplaint.getTitle();
            }

            notificationService.createNotification(
                    admin.getId(),
                    notificationMessage,
                    "NEW_COMPLAINT"
            );

            String technicianInfo;

            if (assignedTechnician != null) {

                technicianInfo =
                        "Automatically Assigned Technician: "
                                + assignedTechnician.getName()
                                + "\n"
                                + "Technician Email: "
                                + assignedTechnician.getEmail()
                                + "\n\n";

            } else {

                technicianInfo =
                        "Technician Assignment: No available technician found.\n"
                                + "Please assign a technician manually.\n\n";
            }

            emailService.sendEmail(
                    admin.getEmail(),
                    "CampusFix - New Complaint #"
                            + savedComplaint.getId(),

                    "Hello " + admin.getName() + ",\n\n"
                            + "A new campus complaint has been submitted.\n\n"

                            + "Complaint ID: #"
                            + savedComplaint.getId() + "\n"

                            + "Title: "
                            + savedComplaint.getTitle() + "\n"

                            + "Category: "
                            + savedComplaint.getCategory() + "\n"

                            + "Priority: "
                            + savedComplaint.getPriority() + "\n"

                            + "Location: "
                            + savedComplaint.getLocation() + "\n\n"

                            + technicianInfo

                            + "Please log in to CampusFix to review the complaint.\n\n"

                            + "CampusFix Support Team"
            );
        }

        // -----------------------------------------------------
        // NOTIFY ASSIGNED TECHNICIAN
        // -----------------------------------------------------

        if (assignedTechnician != null) {

            /*
             * If Technician is not connected to User,
             * we cannot create a user notification here.
             *
             * Email notification is still sent.
             */

            emailService.sendEmail(
                    assignedTechnician.getEmail(),

                    "CampusFix - Complaint Assigned #"
                            + savedComplaint.getId(),

                    "Hello "
                            + assignedTechnician.getName()
                            + ",\n\n"

                            + "A new complaint has been automatically assigned to you.\n\n"

                            + "Complaint ID: #"
                            + savedComplaint.getId()
                            + "\n"

                            + "Title: "
                            + savedComplaint.getTitle()
                            + "\n"

                            + "Category: "
                            + savedComplaint.getCategory()
                            + "\n"

                            + "Priority: "
                            + savedComplaint.getPriority()
                            + "\n"

                            + "Location: "
                            + savedComplaint.getLocation()
                            + "\n\n"

                            + "Please log in to CampusFix and review the complaint.\n\n"

                            + "CampusFix Support Team"
            );
        }

        return savedComplaint;
    }

    // =========================================================
    // GET STUDENT COMPLAINTS
    // =========================================================

    public List<Complaint> getUserComplaints(
            Long userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                )
                        );

        return complaintRepository.findByUser(user);
    }

    // =========================================================
    // GET COMPLAINT
    // =========================================================

    public Complaint getComplaint(
            Long id) {

        return complaintRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Complaint not found"
                        )
                );
    }

    // =========================================================
    // GET ALL COMPLAINTS
    // =========================================================

    public List<Complaint> getAllComplaints() {

        return complaintRepository.findAll();
    }

    // =========================================================
    // EDIT COMPLAINT
    // STUDENT CAN EDIT OWN PENDING COMPLAINT
    // =========================================================

    public Complaint updateComplaint(
            Long complaintId,
            Long userId,
            ComplaintRequest request) {

        return updateComplaint(complaintId, userId, request, null);
    }

    public Complaint updateComplaint(
            Long complaintId,
            Long userId,
            ComplaintRequest request,
            List<MultipartFile> images) {

        // -----------------------------------------------------
        // FIND COMPLAINT
        // -----------------------------------------------------

        Complaint complaint =
                complaintRepository.findById(complaintId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Complaint not found"
                                )
                        );

        // -----------------------------------------------------
        // CHECK COMPLAINT OWNER
        // -----------------------------------------------------

        if (complaint.getUser() == null
                || complaint.getUser().getId() == null
                || !complaint.getUser()
                        .getId()
                        .equals(userId)) {

            throw new RuntimeException(
                    "You are not allowed to edit this complaint"
            );
        }

        // -----------------------------------------------------
        // ONLY PENDING COMPLAINTS CAN BE EDITED
        // -----------------------------------------------------

        if (!"PENDING".equalsIgnoreCase(
                complaint.getStatus())) {

            throw new RuntimeException(
                    "Only pending complaints can be edited"
            );
        }

        // -----------------------------------------------------
        // VALIDATE REQUEST
        // -----------------------------------------------------

        if (request == null) {

            throw new RuntimeException(
                    "Complaint request cannot be empty"
            );
        }

        if (request.getTitle() == null
                || request.getTitle().trim().isEmpty()) {

            throw new RuntimeException(
                    "Complaint title is required"
            );
        }

        if (request.getDescription() == null
                || request.getDescription().trim().isEmpty()) {

            throw new RuntimeException(
                    "Complaint description is required"
            );
        }

        if (request.getLocation() == null
                || request.getLocation().trim().isEmpty()) {

            throw new RuntimeException(
                    "Complaint location is required"
            );
        }

        // -----------------------------------------------------
        // UPDATE TITLE
        // -----------------------------------------------------

        complaint.setTitle(
                request.getTitle().trim()
        );

        // -----------------------------------------------------
        // UPDATE DESCRIPTION
        // -----------------------------------------------------

        complaint.setDescription(
                request.getDescription().trim()
        );

        // -----------------------------------------------------
        // UPDATE LOCATION
        // -----------------------------------------------------

        complaint.setLocation(
                request.getLocation().trim()
        );

        // -----------------------------------------------------
        // UPDATE CATEGORY
        // -----------------------------------------------------

        String category = request.getCategory();

        if (category == null
                || category.trim().isEmpty()) {

            category =
                    aiComplaintService.categorizeComplaint(
                            request.getTitle(),
                            request.getDescription()
                    );
        }

        if (category == null
                || category.trim().isEmpty()) {

            category = "GENERAL";
        }

        complaint.setCategory(
                category.trim().toUpperCase()
        );

        // -----------------------------------------------------
        // UPDATE PRIORITY
        // -----------------------------------------------------

        String priority = request.getPriority();

        if (priority == null
                || priority.trim().isEmpty()) {

            priority =
                    aiComplaintService.determinePriority(
                            request.getTitle(),
                            request.getDescription()
                    );
        }

        if (priority == null
                || priority.trim().isEmpty()) {

            priority = "MEDIUM";
        }

        complaint.setPriority(
                priority.trim().toUpperCase()
        );

        // -----------------------------------------------------
        // UPDATE IMAGE IF PROVIDED
        // -----------------------------------------------------

        if (images != null && !images.isEmpty()) {
            boolean hasValidImage = images.stream().anyMatch(f -> f != null && !f.isEmpty());
            if (hasValidImage) {
                String newImageUrl = fileStorageService.saveImages(images);
                complaint.setImageUrl(newImageUrl);
            }
        }

        // -----------------------------------------------------
        // SAVE UPDATED COMPLAINT
        // -----------------------------------------------------

        Complaint updatedComplaint =
                complaintRepository.save(complaint);

        // -----------------------------------------------------
        // NOTIFY ACTIVE ADMINS
        // -----------------------------------------------------

        List<User> activeAdmins =
                userRepository.findByRoleAndAccountStatus(
                        "ADMIN",
                        "ACTIVE"
                );

        for (User admin : activeAdmins) {

            String notificationMessage =
                    "Complaint #"
                            + updatedComplaint.getId()
                            + " has been updated by the student.";

            notificationService.createNotification(
                    admin.getId(),
                    notificationMessage,
                    "COMPLAINT_UPDATED"
            );

            // -------------------------------------------------
            // EMAIL ADMIN
            // -------------------------------------------------

            emailService.sendEmail(
                    admin.getEmail(),

                    "CampusFix - Complaint Updated #"
                            + updatedComplaint.getId(),

                    "Hello " + admin.getName() + ",\n\n"

                            + "A student has updated a complaint.\n\n"

                            + "Complaint ID: #"
                            + updatedComplaint.getId()
                            + "\n"

                            + "Title: "
                            + updatedComplaint.getTitle()
                            + "\n"

                            + "Category: "
                            + updatedComplaint.getCategory()
                            + "\n"

                            + "Priority: "
                            + updatedComplaint.getPriority()
                            + "\n"

                            + "Location: "
                            + updatedComplaint.getLocation()
                            + "\n"

                            + "Status: "
                            + updatedComplaint.getStatus()
                            + "\n\n"

                            + "Please log in to CampusFix to review "
                            + "the updated complaint.\n\n"

                            + "CampusFix Support Team"
            );
        }

        return updatedComplaint;
    }

    // =========================================================
    // UPDATE STATUS
    // =========================================================

    public Complaint updateComplaintStatus(
            Long id,
            String status) {

        Complaint complaint =
                getComplaint(id);

        if (status == null
                || status.trim().isEmpty()) {

            throw new RuntimeException(
                    "Status is required"
            );
        }

        String normalizedStatus =
                status.trim().toUpperCase();

        if (!normalizedStatus.equals("PENDING")
                && !normalizedStatus.equals("IN_PROGRESS")
                && !normalizedStatus.equals("RESOLVED")
                && !normalizedStatus.equals("REOPENED")) {

            throw new RuntimeException(
                    "Invalid complaint status"
            );
        }

        complaint.setStatus(
                normalizedStatus
        );

        return complaintRepository.save(
                complaint
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deleteComplaint(
            Long id) {

        if (!complaintRepository.existsById(id)) {

            throw new RuntimeException(
                    "Complaint not found"
            );
        }

        complaintRepository.deleteById(id);
    }
}