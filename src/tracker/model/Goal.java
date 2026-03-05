package tracker.model;

/**
 * Represents a student's academic goal.
 */
public class Goal {
    private int id;
    private int studentId;
    private Integer subjectId; // nullable — null means overall average goal
    private double targetScore;
    private String deadline;
    private String status; // ACTIVE, ACHIEVED, ABANDONED
    private String createdAt;

    public Goal() {
    }

    public Goal(int id, int studentId, Integer subjectId, double targetScore,
            String deadline, String status, String createdAt) {
        this.id = id;
        this.studentId = studentId;
        this.subjectId = subjectId;
        this.targetScore = targetScore;
        this.deadline = deadline;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public double getTargetScore() {
        return targetScore;
    }

    public void setTargetScore(double targetScore) {
        this.targetScore = targetScore;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("Goal[target=%.1f, status=%s]", targetScore, status);
    }
}
