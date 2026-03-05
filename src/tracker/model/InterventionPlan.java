package tracker.model;

/**
 * Represents an intervention plan assigned to a student by a teacher.
 */
public class InterventionPlan {
    private int id;
    private int studentId;
    private int teacherId;
    private String interventionType; // TUTORING, PRACTICE, TOPIC_REVIEW, SCHEDULE_CHANGE, STRATEGY_ADJUSTMENT
    private String description;
    private String startDate;
    private String status; // ACTIVE, COMPLETED, CANCELLED

    public InterventionPlan() {
    }

    public InterventionPlan(int id, int studentId, int teacherId, String interventionType,
            String description, String startDate, String status) {
        this.id = id;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.interventionType = interventionType;
        this.description = description;
        this.startDate = startDate;
        this.status = status;
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

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getInterventionType() {
        return interventionType;
    }

    public void setInterventionType(String interventionType) {
        this.interventionType = interventionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("Intervention[%s, status=%s]", interventionType, status);
    }
}
