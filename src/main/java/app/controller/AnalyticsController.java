package app.controller;

import app.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:5173")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(
            AnalyticsService analyticsService
    ) {
        this.analyticsService =
                analyticsService;
    }


    // ==========================================
    // OVERALL ANALYTICS
    // ==========================================

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        return analyticsService
                .getSummary();
    }


    // ==========================================
    // CATEGORY ANALYTICS
    // ==========================================

    @GetMapping("/category/{category}")
    public Map<String, Object> getCategoryAnalytics(
            @PathVariable String category
    ) {

        return analyticsService
                .getCategoryAnalytics(category);
    }
}