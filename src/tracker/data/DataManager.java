package tracker.data;

import tracker.model.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory data store for student records.
 * Provides methods to add, retrieve, and search students.
 * No database or external persistence -- purely in-memory.
 */
public class DataManager {

    private ArrayList<Student> students;

    public DataManager() {
        this.students = new ArrayList<>();
    }

    /**
     * Adds a student to the in-memory store.
     *
     * @param student the Student to add; ignored if null
     */
    public void addStudent(Student student) {
        if (student != null) {
            students.add(student);
        }
    }

    /**
     * Returns the full list of students.
     *
     * @return list of all students
     */
    public List<Student> getStudents() {
        return students;
    }

    /**
     * Finds a student by their unique ID.
     *
     * @param id the student ID to search for
     * @return the matching Student, or null if not found
     */
    public Student findStudentById(String id) {
        if (id == null) {
            return null;
        }
        for (Student student : students) {
            if (id.equals(student.getId())) {
                return student;
            }
        }
        return null;
    }

    /**
     * Checks whether a student with the given ID already exists.
     *
     * @param id the student ID to check
     * @return true if a student with this ID exists
     */
    public boolean studentExists(String id) {
        return findStudentById(id) != null;
    }

    /**
     * Returns all students whose average score is below 50 (at-risk).
     *
     * @return list of at-risk students
     */
    public List<Student> getAtRiskStudents() {
        List<Student> atRisk = new ArrayList<>();
        for (Student student : students) {
            if (student.isAtRisk()) {
                atRisk.add(student);
            }
        }
        return atRisk;
    }
}
