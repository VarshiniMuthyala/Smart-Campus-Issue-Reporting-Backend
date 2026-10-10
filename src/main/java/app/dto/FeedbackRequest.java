package app.dto;

public class FeedbackRequest {

    private int rating;
    private String issueFixed;
    private String comment;
    private boolean reopenRequested;

    public FeedbackRequest() {
    }

    public FeedbackRequest(int rating, String issueFixed, String comment, boolean reopenRequested) {
        this.rating = rating;
        this.issueFixed = issueFixed;
        this.comment = comment;
        this.reopenRequested = reopenRequested;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getIssueFixed() {
        return issueFixed;
    }

    public void setIssueFixed(String issueFixed) {
        this.issueFixed = issueFixed;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isReopenRequested() {
        return reopenRequested;
    }

    public void setReopenRequested(boolean reopenRequested) {
        this.reopenRequested = reopenRequested;
    }
}
