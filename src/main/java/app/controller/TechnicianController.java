package app.controller;

import app.model.Complaint;
import app.model.Technician;
import app.model.User;
import app.service.TechnicianService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
@CrossOrigin(origins = "http://localhost:5173")
public class TechnicianController {

    private final TechnicianService technicianService;

    public TechnicianController(
            TechnicianService technicianService) {

        this.technicianService =
                technicianService;
    }

    // ==========================================
    // ADMIN - GET ALL TECHNICIANS
    // ==========================================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Technician>> getAllTechnicians() {

        return ResponseEntity.ok(
                technicianService.getAllTechnicians()
        );
    }

    // ==========================================
    // ADMIN - GET TECHNICIAN BY ID
    // ==========================================

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTechnicianById(
            @PathVariable Long id) {

        try {

            return ResponseEntity.ok(
                    technicianService.getTechnician(id)
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ==========================================
    // TECHNICIAN - OWN PROFILE
    // ==========================================

    @GetMapping("/me")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> getMyProfile(
            Authentication authentication) {

        try {

            String email =
                    authentication.getName();

            Technician technician =
                    technicianService
                            .getTechnicianByEmail(email);

            return ResponseEntity.ok(
                    technician
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ==========================================
    // TECHNICIAN - MY ASSIGNED COMPLAINTS
    // ==========================================

    @GetMapping("/my-complaints")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> getMyComplaints(
            Authentication authentication) {

        try {

            String email =
                    authentication.getName();

            Technician technician =
                    technicianService.getTechnicianByEmail(email);

            List<Complaint> complaints =
                    technicianService
                            .getAssignedComplaints(technician.getId());

            return ResponseEntity.ok(
                    complaints
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ==========================================
    // TECHNICIAN - UPDATE COMPLAINT STATUS
    // ==========================================

    @PutMapping(
            "/complaints/{complaintId}/status"
    )
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable Long complaintId,
            @RequestParam String status,
            Authentication authentication) {

        try {

            String email =
                    authentication.getName();

            Technician technician =
                    technicianService.getTechnicianByEmail(email);

            Complaint updatedComplaint =
                    technicianService
                            .updateComplaintStatus(
                                    technician.getId(),
                                    complaintId,
                                    status
                            );

            return ResponseEntity.ok(
                    updatedComplaint
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // ==========================================
    // TECHNICIAN - UPDATE AVAILABILITY
    // ==========================================

    @PutMapping("/availability")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<?> updateAvailability(
            @RequestParam boolean available,
            Authentication authentication) {

        try {

            String email =
                    authentication.getName();

            Technician technician =
                    technicianService
                            .updateAvailability(
                                    email,
                                    available
                            );

            return ResponseEntity.ok(
                    technician
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}