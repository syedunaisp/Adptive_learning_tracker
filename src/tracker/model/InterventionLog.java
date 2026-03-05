package tracker.model;

/**
 * Represents a log entry for an intervention plan.
 */
public class InterventionLog {
    private int id;
    private int planId;
    private String notes;
    private String outcomeMetric;
    private String logDate;

    public InterventionLog() {
    }

    public InterventionLog(int id, int planId, String notes, String outcomeMetric, String logDate) {
        this.id = id;
        this.planId = planId;
        this.notes = notes;
        this.outcomeMetric = outcomeMetric;
        this.logDate = logDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getOutcomeMetric() {
        return outcomeMetric;
    }

    public void setOutcomeMetric(String outcomeMetric) {
        this.outcomeMetric = outcomeMetric;
    }

    public String getLogDate() {
        return logDate;
    }

    public void setLogDate(String logDate) {
        this.logDate = logDate;
    }

    @Override
    public String toString() {
        return String.format("Log[plan=%d, date=%s]", planId, logDate);
    }
}
