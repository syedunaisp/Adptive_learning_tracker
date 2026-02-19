package tracker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student with an ID, name, and a list of subjects.
 * Provides methods to calculate average score and detect weak subjects.
 */
public class Student {

    private String id;
    private String name;
    private List<Subject> subjects;

    /**
     * Constructs a Student with the given ID and name.
     * Initializes an empty subject list.
     *
     * @param id   the unique student identifier
     * @param name the student's full name
     */
    public Student(String id, String name) {
        this.id = id;
        this.name = name;
        this.subjects = new ArrayList<>();
    }

    /**
     * Adds a subject to this student's record.
     *
     * @param subject the Subject to add; ignored if null
     */
    public void addSubject(Subject subject) {
        if (subject != null) {
            this.subjects.add(subject);
        }
    }

    /**
     * Calculates the average score across all subjects.
     *
     * @return the average score, or 0.0 if no subjects are recorded
     */
    public double getAverageScore() {
        if (subjects == null || subjects.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (Subject subject : subjects) {
            total += subject.getScore();
        }
        return total / subjects.size();
    }

    /**
     * Returns a list of subjects where the score is below 60.
     * These are considered "weak" subjects that need attention.
     *
     * @return list of weak Subject objects (score < 60)
     */
    public List<Subject> getWeakSubjects() {
        List<Subject> weakSubjects = new ArrayList<>();
        if (subjects == null) {
            return weakSubjects;
        }
        for (Subject subject : subjects) {
            if (subject.getScore() < 60) {
                weakSubjects.add(subject);
            }
        }
        return weakSubjects;
    }

    /**
     * Extracts the names of weak subjects as a list of strings.
     * Convenience method used by the recommendation engine.
     *
     * @return list of weak subject names
     */
    public List<String> getWeakSubjectNames() {
        List<String> names = new ArrayList<>();
        for (Subject subject : getWeakSubjects()) {
            names.add(subject.getSubjectName());
        }
        return names;
    }

    /**
     * Determines whether this student is "at risk".
     * A student is at risk if their average score is below 50.
     *
     * @return true if the student is at risk
     */
    public boolean isAtRisk() {
        return getAverageScore() < 50;
    }

    // --- Getters ---

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    // --- Setters ---

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }

    @Override
    public String toString() {
        return "Student{id='" + id + "', name='" + name + "', avg=" +
                String.format("%.2f", getAverageScore()) + "}";
    }
}
