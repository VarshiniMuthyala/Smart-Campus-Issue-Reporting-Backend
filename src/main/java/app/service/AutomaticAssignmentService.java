package app.service;

import app.model.Complaint;
import app.model.Technician;
import app.repository.TechnicianRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AutomaticAssignmentService {

    private final TechnicianRepository technicianRepository;

    public AutomaticAssignmentService(
            TechnicianRepository technicianRepository) {

        this.technicianRepository = technicianRepository;
    }

    /**
     * Automatically finds the best technician for a complaint.
     *
     * Rules:
     * 1. Technician must be available.
     * 2. Technician specialization should match complaint category.
     * 3. If multiple technicians match, choose the one
     *    having the lowest number of active complaints.
     * 4. If no specialist is available, try GENERAL.
     * 5. If nobody is available, return null.
     */
    public Technician findBestTechnician(Complaint complaint) {

        if (complaint == null) {
            return null;
        }

        // -----------------------------------------------------
        // GET COMPLAINT CATEGORY
        // -----------------------------------------------------

        String category = complaint.getCategory();

        if (category == null
                || category.trim().isEmpty()) {

            category = "GENERAL";
        }

        category = normalizeCategory(category);

        // -----------------------------------------------------
        // FIND AVAILABLE MATCHING TECHNICIANS
        // -----------------------------------------------------

        List<Technician> technicians =
                technicianRepository
                        .findBySpecializationIgnoreCaseAndAvailableTrue(
                                category
                        );

        // -----------------------------------------------------
        // FALLBACK TO GENERAL
        // -----------------------------------------------------

        if (technicians == null
                || technicians.isEmpty()) {

            if (!"GENERAL".equalsIgnoreCase(category)) {

                technicians =
                        technicianRepository
                                .findBySpecializationIgnoreCaseAndAvailableTrue(
                                        "GENERAL"
                                );
            }
        }

        // -----------------------------------------------------
        // NO TECHNICIAN AVAILABLE
        // -----------------------------------------------------

        if (technicians == null
                || technicians.isEmpty()) {

            return null;
        }

        // -----------------------------------------------------
        // FIND TECHNICIAN WITH LOWEST WORKLOAD
        // -----------------------------------------------------

        Technician bestTechnician = null;

        long lowestWorkload = Long.MAX_VALUE;

        for (Technician technician : technicians) {

            if (technician == null) {
                continue;
            }

            long workload =
                    technicianRepository.countActiveComplaints(
                            technician.getId()
                    );

            if (bestTechnician == null
                    || workload < lowestWorkload) {

                bestTechnician = technician;
                lowestWorkload = workload;
            }

            /*
             * If workload is equal, we keep the technician
             * already selected.
             *
             * This gives a simple deterministic selection.
             */
        }

        return bestTechnician;
    }

    // =========================================================
    // NORMALIZE CATEGORY
    // =========================================================

    private String normalizeCategory(String category) {

        String value =
                category
                        .trim()
                        .toUpperCase()
                        .replace("-", "_");

        switch (value) {

            case "ELECTRICAL":
                return "ELECTRICAL";

            case "PLUMBING":
                return "PLUMBING";

            case "NETWORK":
                return "NETWORK";

            case "FURNITURE":
                return "FURNITURE";

            case "CIVIL":
                return "CIVIL";

            case "GENERAL_MAINTENANCE":
                return "GENERAL MAINTENANCE";

            case "GENERAL":
                return "GENERAL";

            default:
                return "GENERAL";
        }
    }
}