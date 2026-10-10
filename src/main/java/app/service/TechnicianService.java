package app.service;

import app.model.Complaint;
import app.model.Technician;
import app.model.User;
import app.repository.ComplaintRepository;
import app.repository.TechnicianRepository;
import app.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TechnicianService {

    private final TechnicianRepository technicianRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public TechnicianService(
            TechnicianRepository technicianRepository,
            ComplaintRepository complaintRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            EmailService emailService) {

        this.technicianRepository =
                technicianRepository;

        this.complaintRepository =
                complaintRepository;

        this.userRepository =
                userRepository;

        this.notificationService =
                notificationService;

        this.emailService =
                emailService;
    }

    // =========================================================
    // CREATE TECHNICIAN
    // =========================================================

    public Technician createTechnician(
            Technician technician) {

        if (technician == null) {

            throw new RuntimeException(
                    "Technician data cannot be empty"
            );
        }

        if (technician.getEmail() == null
                || technician.getEmail()
                        .trim()
                        .isEmpty()) {

            throw new RuntimeException(
                    "Technician email is required"
            );
        }

        String email =
                technician.getEmail()
                        .trim()
                        .toLowerCase();

        if (technicianRepository
                .existsByEmail(email)) {

            throw new RuntimeException(
                    "Technician email already exists"
            );
        }

        technician.setEmail(email);

        // New technicians are available by default
        technician.setAvailable(true);

        return technicianRepository.save(
                technician
        );
    }

    // =========================================================
    // GET ALL TECHNICIANS
    // =========================================================

    public List<Technician> getAllTechnicians() {

        return technicianRepository.findAll();
    }

    // =========================================================
    // GET TECHNICIAN BY ID
    // =========================================================

    public Technician getTechnician(
            Long id) {

        return technicianRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Technician not found"
                        )
                );
    }

    // =========================================================
    // GET TECHNICIAN BY EMAIL
    // =========================================================

    public Technician getTechnicianByEmail(
            String email) {

        if (email == null
                || email.trim().isEmpty()) {

            throw new RuntimeException(
                    "Technician email is required"
            );
        }

        return technicianRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Technician profile not found"
                        )
                );
    }

    // =========================================================
    // GET ASSIGNED COMPLAINTS
    // =========================================================

    public List<Complaint> getAssignedComplaints(
            Long technicianId) {

        Technician technician =
                getTechnician(
                        technicianId
                );

        return complaintRepository
                .findByTechnician(
                        technician
                );
    }

    // =========================================================
    // UPDATE COMPLAINT STATUS
    // =========================================================

    public Complaint updateComplaintStatus(
            Long technicianId,
            Long complaintId,
            String status) {

        Technician technician =
                getTechnician(
                        technicianId
                );

        Complaint complaint =
                complaintRepository
                        .findById(complaintId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Complaint not found"
                                )
                        );

        // -----------------------------------------------------
        // CHECK ASSIGNMENT
        // -----------------------------------------------------

        if (complaint.getTechnician() == null
                || complaint
                        .getTechnician()
                        .getId() == null
                || !complaint
                        .getTechnician()
                        .getId()
                        .equals(
                                technician.getId()
                        )) {

            throw new RuntimeException(
                    "Complaint is not assigned to this technician"
            );
        }

        // -----------------------------------------------------
        // VALIDATE STATUS
        // -----------------------------------------------------

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
                && !normalizedStatus.equals("RESOLVED")) {

            throw new RuntimeException(
                    "Invalid complaint status"
            );
        }

        String oldStatus =
                complaint.getStatus();

        complaint.setStatus(
                normalizedStatus
        );

        Complaint savedComplaint =
                complaintRepository.save(
                        complaint
                );

        // -----------------------------------------------------
        // NOTIFY STUDENT
        // -----------------------------------------------------

        if (complaint.getUser() != null
                && !normalizedStatus.equals(
                        oldStatus)) {

            String message;

            if ("RESOLVED".equals(
                    normalizedStatus)) {

                message =
                        "Your complaint #"
                                + complaint.getId()
                                + " has been resolved by "
                                + technician.getName()
                                + ".";

            } else if ("IN_PROGRESS".equals(
                    normalizedStatus)) {

                message =
                        "Your complaint #"
                                + complaint.getId()
                                + " is now being handled by "
                                + technician.getName()
                                + ".";

            } else {

                message =
                        "Your complaint #"
                                + complaint.getId()
                                + " is pending.";
            }

            String type;

            if ("RESOLVED".equals(
                    normalizedStatus)) {

                type = "COMPLAINT_RESOLVED";

            } else {

                type =
                        "COMPLAINT_STATUS_UPDATED";
            }

            notificationService.createNotification(
                    complaint.getUser().getId(),
                    message,
                    type
            );

            emailService.sendEmail(
                    complaint.getUser().getEmail(),
                    "CampusFix - Complaint #" + complaint.getId() + " Status Update",
                    "Hello " + complaint.getUser().getName() + ",\n\n"
                            + message + "\n\n"
                            + "Complaint: " + complaint.getTitle() + "\n"
                            + "Technician: " + technician.getName() + "\n\n"
                            + "CampusFix Support Team"
            );
        }

        return savedComplaint;
    }

    // =========================================================
    // UPDATE AVAILABILITY
    // =========================================================

    public Technician updateAvailability(
            String email,
            boolean available) {

        Technician technician =
                getTechnicianByEmail(
                        email
                );

        technician.setAvailable(
                available
        );

        return technicianRepository.save(
                technician
        );
    }

    // =========================================================
    // GET TECHNICIAN USER
    // =========================================================

    public User getTechnicianUser(
            String email) {

        return userRepository
                .findByEmail(
                        email.trim().toLowerCase()
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Technician user account not found"
                        )
                );
    }
}