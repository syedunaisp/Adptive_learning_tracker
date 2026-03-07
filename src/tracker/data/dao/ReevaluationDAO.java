package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.ReevaluationRequest;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'reevaluation_requests' table.
 */
public class ReevaluationDAO {

    public boolean submitRequest(String subjectName, int studentDbId, String reason, int teacherId) {
        // score_id is a legacy NOT NULL column with FK — we bypass it by temporarily
        // disabling FK checks
        String sql = "INSERT INTO reevaluation_requests (score_id, student_id, reason, subject_name, teacher_id) " +
                "VALUES (0, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            try (java.sql.Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys=OFF");
            }
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ps.setString(2, reason);
            ps.setString(3, subjectName);
            ps.setInt(4, teacherId);
            boolean result = ps.executeUpdate() > 0;
            try (java.sql.Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys=ON");
            }
            return result;
        } catch (SQLException e) {
            System.err.println("ReevaluationDAO.submitRequest error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public List<ReevaluationRequest> getRequestsByStudent(int studentDbId) {
        String sql = "SELECT id, score_id, student_id, reason, status, submitted_at, " +
                "resolved_by, resolution_notes, resolved_at, subject_name, teacher_id, updated_marks " +
                "FROM reevaluation_requests WHERE student_id = ? ORDER BY submitted_at DESC";
        return queryRequests(sql, studentDbId);
    }

    public List<ReevaluationRequest> getPendingRequestsForTeacher(int teacherUserId) {
        String sql = "SELECT id, score_id, student_id, reason, status, submitted_at, " +
                "resolved_by, resolution_notes, resolved_at, subject_name, teacher_id, updated_marks " +
                "FROM reevaluation_requests WHERE status = 'PENDING' AND teacher_id = ? ORDER BY submitted_at ASC";
        return queryRequests(sql, teacherUserId);
    }

    public boolean resolveRequest(int requestId, int resolvedByUserId, String notes, double updatedMarks) {
        return updateRequestStatus(requestId, "RESOLVED", resolvedByUserId, notes, updatedMarks);
    }

    public boolean rejectRequest(int requestId, int resolvedByUserId, String notes) {
        return updateRequestStatus(requestId, "REJECTED", resolvedByUserId, notes, -1);
    }

    private boolean updateRequestStatus(int requestId, String status, int resolvedByUserId, String notes,
            double updatedMarks) {
        String sql = "UPDATE reevaluation_requests SET status = ?, resolved_by = ?, " +
                "resolution_notes = ?, resolved_at = datetime('now'), updated_marks = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, resolvedByUserId);
            ps.setString(3, notes);
            if (updatedMarks >= 0) {
                ps.setDouble(4, updatedMarks);
            } else {
                ps.setNull(4, Types.REAL);
            }
            ps.setInt(5, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ReevaluationDAO.updateRequestStatus error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    private List<ReevaluationRequest> queryRequests(String sql, int paramId) {
        List<ReevaluationRequest> requests = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, paramId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                requests.add(mapRequest(rs));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ReevaluationDAO.queryRequests error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return requests;
    }

    private ReevaluationRequest mapRequest(ResultSet rs) throws SQLException {
        int resolvedById = rs.getInt("resolved_by");
        Integer resolvedBy = rs.wasNull() ? null : resolvedById;

        int tid = rs.getInt("teacher_id");
        Integer teacherId = rs.wasNull() ? null : tid;

        double um = rs.getDouble("updated_marks");
        Double updatedMarks = rs.wasNull() ? null : um;

        return new ReevaluationRequest(
                rs.getInt("id"), rs.getInt("score_id"), rs.getInt("student_id"),
                rs.getString("reason"), rs.getString("status"), rs.getString("submitted_at"),
                resolvedBy, rs.getString("resolution_notes"), rs.getString("resolved_at"),
                rs.getString("subject_name"), teacherId, updatedMarks);
    }

    /**
     * Looks up the student's class teacher user ID.
     * Falls back to any teacher in the system if the student has no class.
     * Returns -1 if no teacher found at all.
     */
    public int getStudentTeacherId(int studentDbId) {
        // Primary: look up via class assignment
        String sql = "SELECT c.teacher_id FROM classes c " +
                "JOIN students s ON s.class_id = c.id " +
                "WHERE s.id = ? AND c.teacher_id IS NOT NULL";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int teacherId = rs.getInt("teacher_id");
                boolean wasNull = rs.wasNull();
                rs.close();
                if (!wasNull && teacherId > 0)
                    return teacherId;
            } else {
                rs.close();
            }

            // Fallback: find any teacher user in the system
            DBConnectionManager.closeQuietly(ps);
            ps = conn.prepareStatement("SELECT id FROM users WHERE role_id = 2 AND enabled = 1 LIMIT 1");
            rs = ps.executeQuery();
            if (rs.next()) {
                int teacherId = rs.getInt("id");
                rs.close();
                return teacherId;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ReevaluationDAO.getStudentTeacherId error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    /**
     * Gets the student name by their DB id.
     */
    public String getStudentNameById(int studentDbId) {
        String sql = "SELECT name FROM students WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("name");
                rs.close();
                return name;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ReevaluationDAO.getStudentNameById error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return "Unknown";
    }
}
