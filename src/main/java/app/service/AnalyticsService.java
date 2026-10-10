package app.service;

import app.repository.ComplaintRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AnalyticsService {

    private final ComplaintRepository complaintRepository;

    public AnalyticsService(
            ComplaintRepository complaintRepository) {

        this.complaintRepository =
                complaintRepository;
    }

    // ==========================================
    // ANALYTICS SUMMARY
    // ==========================================

    public Map<String, Object> getSummary() {

        Map<String, Object> analytics =
                new LinkedHashMap<>();

        // --------------------------------------
        // STATUS COUNTS
        // --------------------------------------

        long total =
                complaintRepository.count();

        long pending =
                complaintRepository
                        .countByStatus("PENDING");

        long inProgress =
                complaintRepository
                        .countByStatus("IN_PROGRESS");

        long resolved =
                complaintRepository
                        .countByStatus("RESOLVED");

        // --------------------------------------
        // CATEGORY COUNTS
        // --------------------------------------

        long electrical =
                complaintRepository
                        .countByCategory("ELECTRICAL");

        long plumbing =
                complaintRepository
                        .countByCategory("PLUMBING");

        long network =
                complaintRepository
                        .countByCategory("NETWORK");

        long furniture =
                complaintRepository
                        .countByCategory("FURNITURE");

        long civil =
                complaintRepository
                        .countByCategory("CIVIL");

        long general =
                complaintRepository
                        .countByCategory("GENERAL");

        long generalMaintenance =
                complaintRepository
                        .countByCategory(
                                "GENERAL MAINTENANCE"
                        );

        // --------------------------------------
        // RESPONSE
        // --------------------------------------

        analytics.put(
                "totalComplaints",
                total
        );

        analytics.put(
                "pendingComplaints",
                pending
        );

        analytics.put(
                "inProgressComplaints",
                inProgress
        );

        analytics.put(
                "resolvedComplaints",
                resolved
        );

        analytics.put(
                "electricalComplaints",
                electrical
        );

        analytics.put(
                "plumbingComplaints",
                plumbing
        );

        analytics.put(
                "networkComplaints",
                network
        );

        analytics.put(
                "furnitureComplaints",
                furniture
        );

        analytics.put(
                "civilComplaints",
                civil
        );

        analytics.put(
                "generalComplaints",
                general
        );

        analytics.put(
                "generalMaintenanceComplaints",
                generalMaintenance
        );

        // --------------------------------------
        // RESOLUTION PERCENTAGE
        // --------------------------------------

        double resolutionPercentage = 0;

        if (total > 0) {

            resolutionPercentage =
                    ((double) resolved / total) * 100;
        }

        analytics.put(
                "resolutionPercentage",
                Math.round(resolutionPercentage)
        );

        return analytics;
    }

    // ==========================================
    // CATEGORY ANALYTICS
    // ==========================================

    public Map<String, Object> getCategoryAnalytics(
            String category) {

        if (category == null ||
                category.trim().isEmpty()) {

            throw new RuntimeException(
                    "Category is required."
            );
        }

        category =
                category.trim()
                        .toUpperCase();

        long count =
                complaintRepository
                        .countByCategory(category);

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "category",
                category
        );

        result.put(
                "complaintCount",
                count
        );

        return result;
    }
}