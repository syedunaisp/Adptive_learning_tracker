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
    private String subjectName;
    private Integer teacherId;
    private Double updatedMarks;

    public ReevaluationRequest() {
    }

    public ReevaluationRequest(int id, int scoreId, int studentId, String reason,
            String status, String submittedAt, Integer resolvedBy,
            String resolutionNotes, String resolvedAt,
            String subjectName, Integer teacherId, Double updatedMarks) {
        this.id = id;
        this.scoreId = scoreId;
        this.studentId = studentId;
        this.reason = reason;
        this.status = status;
        this.submittedAt = submittedAt;
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = resolvedAt;
        this.subjectName = subjectName;
        this.teacherId = teacherId;
        this.updatedMarks = updatedMarks;
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

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public Double getUpdatedMarks() {
        return updatedMarks;
    }

    public void setUpdatedMarks(Double updatedMarks) {
        this.updatedMarks = updatedMarks;
    }

    @Override
    public String toString() {
        return String.format("Reeval[subject=%s, status=%s]", subjectName, status);
    }
}
