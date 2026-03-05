package tracker.model;

/**
 * Represents a student's request for grade re-evaluation.
 */
public class ReevaluationRequest {
    private int id;
    private int scoreId;
    private int studentId;
    private String reason;
    private String status; // PENDING, RESOLVED, REJECTED
    private String submittedAt;
    private Integer resolvedBy; // nullable — user ID of resolver
    private String resolutionNotes;
    private String resolvedAt;

    public ReevaluationRequest() {
    }

    public ReevaluationRequest(int id, int scoreId, int studentId, String reason,
            String status, String submittedAt, Integer resolvedBy,
            String resolutionNotes, String resolvedAt) {
        this.id = id;
        this.scoreId = scoreId;
        this.studentId = studentId;
        this.reason = reason;
        this.status = status;
        this.submittedAt = submittedAt;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = resolvedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScoreId() {
        return scoreId;
    }

    public void setScoreId(int scoreId) {
        this.scoreId = scoreId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(String submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Integer resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public String getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(String resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Override
    public String toString() {
        return String.format("Reeval[score=%d, status=%s]", scoreId, status);
    }
}
