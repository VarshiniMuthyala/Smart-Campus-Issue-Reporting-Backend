package app.controller;

import app.model.Complaint;
import app.model.RoleRequest;
import app.service.AdminService;
import app.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private final AdminService adminService;
    private final AuthService authService;

    public AdminController(
            AdminService adminService,
            AuthService authService) {

        this.adminService = adminService;
        this.authService = authService;
    }

    // =========================================================
    // GET ALL COMPLAINTS
    // =========================================================

    @GetMapping("/complaints")
    public ResponseEntity<?> getAllComplaints() {

        try {

            return ResponseEntity.ok(
                    adminService.getAllComplaints()
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // UPDATE COMPLAINT STATUS
    // =========================================================

    @PutMapping("/complaints/{id}/status")
    public ResponseEntity<?> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam String status) {

        try {

            Complaint complaint =
                    adminService.updateComplaintStatus(
                            id,
                            status
                    );

            return ResponseEntity.ok(
                    complaint
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // ASSIGN TECHNICIAN
    // =========================================================

    @PutMapping(
            "/complaints/{complaintId}/assign/{technicianId}"
    )
    public ResponseEntity<?> assignTechnician(
            @PathVariable Long complaintId,
            @PathVariable Long technicianId) {

        try {

            Complaint complaint =
                    adminService.assignTechnician(
                            complaintId,
                            technicianId
                    );

            return ResponseEntity.ok(
                    complaint
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // GET ALL TECHNICIANS
    // =========================================================

    @GetMapping("/technicians")
    public ResponseEntity<?> getAllTechnicians() {

        try {

            return ResponseEntity.ok(
                    authService.getAllTechnicians()
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // GET PENDING ROLE REQUESTS
    // =========================================================

    @GetMapping("/requests")
    public ResponseEntity<?> getPendingRequests() {

        try {

            List<RoleRequest> requests =
                    authService.getPendingRoleRequests();

            return ResponseEntity.ok(
                    requests
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // APPROVE ROLE REQUEST
    // =========================================================

    @PutMapping("/requests/{requestId}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        try {

            // Get logged-in admin email from JWT
            String adminEmail =
                    authentication.getName();

            RoleRequest request =
                    authService.approveRoleRequest(
                            requestId,
                            adminEmail
                    );

            return ResponseEntity.ok(
                    request
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // =========================================================
    // REJECT ROLE REQUEST
    // =========================================================

    @PutMapping("/requests/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long requestId,
            Authentication authentication) {

        try {

            // Get logged-in admin email from JWT
            String adminEmail =
                    authentication.getName();

            RoleRequest request =
                    authService.rejectRoleRequest(
                            requestId,
                            adminEmail
                    );

            return ResponseEntity.ok(
                    request
            );

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }
}