package app.dto;

public class AnalyticsResponse {

    private long totalComplaints;
    private long pendingComplaints;
    private long inProgressComplaints;
    private long resolvedComplaints;

    private long electricalComplaints;
    private long plumbingComplaints;
    private long networkComplaints;
    private long furnitureComplaints;
    private long civilComplaints;
    private long generalComplaints;


    // ==========================================
    // DEFAULT CONSTRUCTOR
    // ==========================================

    public AnalyticsResponse() {
    }


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public AnalyticsResponse(
            long totalComplaints,
            long pendingComplaints,
            long inProgressComplaints,
            long resolvedComplaints,
            long electricalComplaints,
            long plumbingComplaints,
            long networkComplaints,
            long furnitureComplaints,
            long civilComplaints,
            long generalComplaints) {

        this.totalComplaints = totalComplaints;
        this.pendingComplaints = pendingComplaints;
        this.inProgressComplaints = inProgressComplaints;
        this.resolvedComplaints = resolvedComplaints;

        this.electricalComplaints = electricalComplaints;
        this.plumbingComplaints = plumbingComplaints;
        this.networkComplaints = networkComplaints;
        this.furnitureComplaints = furnitureComplaints;
        this.civilComplaints = civilComplaints;
        this.generalComplaints = generalComplaints;
    }


    // ==========================================
    // GETTERS
    // ==========================================

    public long getTotalComplaints() {
        return totalComplaints;
    }

    public long getPendingComplaints() {
        return pendingComplaints;
    }

    public long getInProgressComplaints() {
        return inProgressComplaints;
    }

    public long getResolvedComplaints() {
        return resolvedComplaints;
    }

    public long getElectricalComplaints() {
        return electricalComplaints;
    }

    public long getPlumbingComplaints() {
        return plumbingComplaints;
    }

    public long getNetworkComplaints() {
        return networkComplaints;
    }

    public long getFurnitureComplaints() {
        return furnitureComplaints;
    }

    public long getCivilComplaints() {
        return civilComplaints;
    }

    public long getGeneralComplaints() {
        return generalComplaints;
    }
}