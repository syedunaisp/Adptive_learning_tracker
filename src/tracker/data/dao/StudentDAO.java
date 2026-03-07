package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.Student;
import tracker.model.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'students' table.
 *
 * Handles:
 * - CRUD for student records
 * - Loading students with their subjects and scores
 * - Filtered queries (by ID, by risk status)
 */
public class StudentDAO {

    /**
     * Inserts a new student record. Returns the DB-generated primary key.
     */
    public int insert(String studentId, String name) {
        String sql = "INSERT INTO students (student_id, name) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, studentId);
            ps.setString(2, name);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("StudentDAO.insert error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    /**
     * Inserts a new student record with Phase 6 fields. Returns the DB-generated
     * primary key.
     */
    public int insert(Student student) {
        String sql = "INSERT INTO students (student_id, name, class_id, roll_number, email) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, student.getId());
            ps.setString(2, student.getName());
            if (student.getClassId() > 0) {
                ps.setInt(3, student.getClassId());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, student.getRollNumber());
            ps.setString(5, student.getEmail());
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("StudentDAO.insert(Student) error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    /**
     * Checks if a student with the given business student_id exists.
     */
    public boolean existsByStudentId(String studentId) {
        String sql = "SELECT COUNT(*) FROM students WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("StudentDAO.existsByStudentId error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return false;
    }

    /**
     * Finds the DB primary key for a business student_id.
     * Returns -1 if not found.
     */
    public int findDbIdByStudentId(String studentId) {
        String sql = "SELECT id FROM students WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("StudentDAO.findDbIdByStudentId error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    /**
     * Finds the name for a business student_id.
     */
    public String findNameByStudentId(String studentId) {
        String sql = "SELECT name FROM students WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            rs = ps.executeQuery();
            if (rs.next())
                return rs.getString("name");
        } catch (SQLException e) {
            System.err.println("StudentDAO.findNameByStudentId error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return null;
    }

    /**
     * Loads ALL students with their subjects fully populated from DB.
     * Uses a joined query for efficiency.
     */
    public List<Student> loadAllWithScores() {
        String sql = "SELECT s.student_id, s.name, s.class_id, s.roll_number, s.email, sub.subject_name, sc.score " +
                "FROM students s " +
                "LEFT JOIN student_scores sc ON s.id = sc.student_id " +
                "LEFT JOIN subjects sub ON sc.subject_id = sub.id " +
                "ORDER BY s.student_id, sub.subject_name";
        List<Student> students = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            String lastId = null;
            Student current = null;
            while (rs.next()) {
                String sid = rs.getString("student_id");
                if (!sid.equals(lastId)) {
                    current = new Student(sid, rs.getString("name"));
                    current.setClassId(rs.getInt("class_id"));
                    current.setRollNumber(rs.getString("roll_number"));
                    current.setEmail(rs.getString("email"));
                    students.add(current);
                    lastId = sid;
                }
                String subName = rs.getString("subject_name");
                if (subName != null) {
                    current.addSubject(new Subject(subName, rs.getDouble("score")));
                }
            }
        } catch (SQLException e) {
            System.err.println("StudentDAO.loadAllWithScores error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return students;
    }

    /**
     * Loads a single student with all scores by business student_id.
     */
    public Student loadByStudentId(String studentId) {
        String sql = "SELECT s.student_id, s.name, s.class_id, s.roll_number, s.email, sub.subject_name, sc.score " +
                "FROM students s " +
                "LEFT JOIN student_scores sc ON s.id = sc.student_id " +
                "LEFT JOIN subjects sub ON sc.subject_id = sub.id " +
                "WHERE s.student_id = ? " +
                "ORDER BY sub.subject_name";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            rs = ps.executeQuery();

            Student student = null;
            while (rs.next()) {
                if (student == null) {
                    student = new Student(rs.getString("student_id"), rs.getString("name"));
                    student.setClassId(rs.getInt("class_id"));
                    student.setRollNumber(rs.getString("roll_number"));
                    student.setEmail(rs.getString("email"));
                }
                String subName = rs.getString("subject_name");
                if (subName != null) {
                    student.addSubject(new Subject(subName, rs.getDouble("score")));
                }
            }
            return student;
        } catch (SQLException e) {
            System.err.println("StudentDAO.loadByStudentId error: " + e.getMessage());
            return null;
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Updates a student's name, roll number, and email by their business
     * student_id.
     */
    public boolean updateStudent(String studentId, String newName, String rollNumber, String email) {
        String sql = "UPDATE students SET name = ?, roll_number = ?, email = ? WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newName);
            ps.setString(2, rollNumber);
            ps.setString(3, email);
            ps.setString(4, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("StudentDAO.updateStudent error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Updates a student's name by their business student_id (backwards
     * compatibility).
     */
    public boolean updateName(String studentId, String newName) {
        String sql = "UPDATE students SET name = ? WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, newName);
            ps.setString(2, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("StudentDAO.updateName error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Deletes a student and all associated scores (cascading).
     */
    public boolean deleteByStudentId(String studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, studentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("StudentDAO.deleteByStudentId error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Returns total student count.
     */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM students";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("StudentDAO.countAll error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return 0;
    }
}
