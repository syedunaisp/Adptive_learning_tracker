package tracker.data.dao;

import tracker.data.DBConnectionManager;

import java.sql.*;

/**
 * Data Access Object for the 'subjects' table.
 *
 * Handles:
 *   - Insert-or-get pattern (ensures subject_name uniqueness)
 *   - Category management
 */
public class SubjectDAO {

    /**
     * Finds or creates a subject by name, returning its DB primary key.
     * If the subject doesn't exist, inserts it with the given category.
     */
    public int findOrCreate(String subjectName, String category) {
        // Try to find existing
        int existing = findIdByName(subjectName);
        if (existing > 0) return existing;

        // Insert new
        String sql = "INSERT INTO subjects (subject_name, category) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, subjectName);
            ps.setString(2, category != null ? category : "UNCATEGORIZED");
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            // Might be a race condition — try find again
            int retry = findIdByName(subjectName);
            if (retry > 0) return retry;
            System.err.println("SubjectDAO.findOrCreate error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    /**
     * Finds a subject's DB primary key by name.
     * Returns -1 if not found.
     */
    public int findIdByName(String subjectName) {
        String sql = "SELECT id FROM subjects WHERE subject_name = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, subjectName);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("SubjectDAO.findIdByName error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    /**
     * Updates the category of a subject.
     */
    public boolean updateCategory(String subjectName, String category) {
        String sql = "UPDATE subjects SET category = ? WHERE subject_name = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, category);
            ps.setString(2, subjectName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("SubjectDAO.updateCategory error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }
}
