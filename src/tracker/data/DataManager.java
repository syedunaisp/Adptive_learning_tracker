package tracker.data;

import tracker.data.dao.StudentDAO;
import tracker.data.dao.SubjectDAO;
import tracker.data.dao.ScoreDAO;
import tracker.model.Student;
import tracker.model.Subject;
import tracker.model.SubjectCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates data access for student records.
 *
 * MIGRATED: Now delegates all persistence to DAO classes backed by SQLite.
 * Maintains an in-memory cache that is refreshed from DB on each load.
 * The public API is preserved so existing service/UI code continues to work.
 *
 * Old in-memory-only behavior is replaced by:
 *   - StudentDAO for student CRUD
 *   - SubjectDAO for subject management
 *   - ScoreDAO for score recording
 */
public class DataManager {

    private final StudentDAO studentDAO;
    private final SubjectDAO subjectDAO;
    private final ScoreDAO scoreDAO;

    /** In-memory cache, refreshed from DB. */
    private List<Student> cachedStudents;

    public DataManager() {
        this.studentDAO = new StudentDAO();
        this.subjectDAO = new SubjectDAO();
        this.scoreDAO = new ScoreDAO();
        this.cachedStudents = new ArrayList<>();
        refreshCache();
    }

    /**
     * Adds a student to the database if new.
     * If the student already exists (by business student_id), this is a no-op.
     *
     * @param student the Student to add; ignored if null
     */
    public void addStudent(Student student) {
        if (student == null) return;
        if (!studentDAO.existsByStudentId(student.getId())) {
            studentDAO.insert(student.getId(), student.getName());
        }
        refreshCache();
    }

    /**
     * Adds a subject score for a student. Persists to DB.
     * Creates the student and subject records if they don't exist.
     *
     * @param studentId   the business student ID
     * @param studentName the student's name (used only if new)
     * @param subjectName the subject name
     * @param score       the score (0-100)
     */
    public void addSubjectScore(String studentId, String studentName,
                                 String subjectName, double score) {
        // Ensure student exists
        if (!studentDAO.existsByStudentId(studentId)) {
            studentDAO.insert(studentId, studentName);
        }
        int studentDbId = studentDAO.findDbIdByStudentId(studentId);
        if (studentDbId < 0) return;

        // Ensure subject exists
        String category = SubjectCategory.categorize(subjectName).name();
        int subjectDbId = subjectDAO.findOrCreate(subjectName, category);
        if (subjectDbId < 0) return;

        // Insert score
        scoreDAO.insertScore(studentDbId, subjectDbId, score);

        // Refresh cache
        refreshCache();
    }

    /**
     * Returns the full list of students (from cache, backed by DB).
     *
     * @return list of all students with their subjects populated
     */
    public List<Student> getStudents() {
        return cachedStudents;
    }

    /**
     * Finds a student by their unique business ID.
     *
     * @param id the student ID to search for
     * @return the matching Student, or null if not found
     */
    public Student findStudentById(String id) {
        if (id == null) return null;
        for (Student student : cachedStudents) {
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
        for (Student student : cachedStudents) {
            if (student.isAtRisk()) {
                atRisk.add(student);
            }
        }
        return atRisk;
    }

    /**
     * Forces a refresh of the in-memory cache from the database.
     */
    public void refreshCache() {
        this.cachedStudents = studentDAO.loadAllWithScores();
    }

    /**
     * Returns the StudentDAO for direct access when needed.
     */
    public StudentDAO getStudentDAO() {
        return studentDAO;
    }

    /**
     * Returns the SubjectDAO for direct access when needed.
     */
    public SubjectDAO getSubjectDAO() {
        return subjectDAO;
    }

    /**
     * Returns the ScoreDAO for direct access when needed.
     */
    public ScoreDAO getScoreDAO() {
        return scoreDAO;
    }
}
