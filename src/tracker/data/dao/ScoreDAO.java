package tracker.data.dao;

import tracker.data.DBConnectionManager;

import java.sql.*;

/**
 * Data Access Object for the 'student_scores' table.
 *
 * Handles:
 * - Recording subject scores for students
 * - Score queries and updates
 */
public class ScoreDAO {

    public boolean insertScore(int studentDbId, int subjectDbId, double score) {
        Connection conn = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUpdate = null;
        PreparedStatement psInsert = null;
        ResultSet rs = null;

        String checkSql = "SELECT id FROM student_scores WHERE student_id = ? AND subject_id = ?";
        String updateSql = "UPDATE student_scores SET score = ?, recorded_at = datetime('now') WHERE id = ?";
        String insertSql = "INSERT INTO student_scores (student_id, subject_id, score) VALUES (?, ?, ?)";

        try {
            conn = DBConnectionManager.getConnection();

            // 1. Check if score already exists for this student + subject
            psCheck = conn.prepareStatement(checkSql);
            psCheck.setInt(1, studentDbId);
            psCheck.setInt(2, subjectDbId);
            rs = psCheck.executeQuery();

            if (rs.next()) {
                // Already exists -> Update
                int scoreId = rs.getInt("id");
                psUpdate = conn.prepareStatement(updateSql);
                psUpdate.setDouble(1, score);
                psUpdate.setInt(2, scoreId);
                return psUpdate.executeUpdate() > 0;
            } else {
                // Doesn't exist -> Insert
                psInsert = conn.prepareStatement(insertSql);
                psInsert.setInt(1, studentDbId);
                psInsert.setInt(2, subjectDbId);
                psInsert.setDouble(3, score);
                return psInsert.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("ScoreDAO.insertScore error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(psCheck);
            DBConnectionManager.closeQuietly(psUpdate);
            DBConnectionManager.closeQuietly(psInsert);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Updates an existing score for a student+subject combination.
     * Updates the most recent score if multiple exist.
     */
    public boolean updateScore(int studentDbId, int subjectDbId, double newScore) {
        String sql = "UPDATE student_scores SET score = ?, recorded_at = datetime('now') " +
                "WHERE id = (SELECT id FROM student_scores " +
                "WHERE student_id = ? AND subject_id = ? ORDER BY recorded_at DESC LIMIT 1)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setDouble(1, newScore);
            ps.setInt(2, studentDbId);
            ps.setInt(3, subjectDbId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ScoreDAO.updateScore error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Deletes all scores for a given student (used before student deletion).
     */
    public boolean deleteByStudentDbId(int studentDbId) {
        String sql = "DELETE FROM student_scores WHERE student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("ScoreDAO.deleteByStudentDbId error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }
}
