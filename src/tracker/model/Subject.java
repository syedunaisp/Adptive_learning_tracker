package tracker.model;

/**
 * Represents an academic subject with a name and score.
 * Used to track individual subject performance for a student.
 */
public class Subject {

    private String subjectName;
    private double score;

    /**
     * Constructs a Subject with the given name and score.
     *
     * @param subjectName the name of the subject
     * @param score       the score achieved (0-100)
     */
    public Subject(String subjectName, double score) {
        this.subjectName = subjectName;
        this.score = score;
    }

    // --- Getters ---

    public String getSubjectName() {
        return subjectName;
    }

    public double getScore() {
        return score;
    }

    // --- Setters ---

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return subjectName + " (" + score + ")";
    }
}
