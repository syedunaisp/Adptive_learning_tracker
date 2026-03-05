package tracker.data.dao;

import tracker.data.DBConnectionManager;

import java.sql.*;

/**
 * Data Access Object for the 'student_scores' table.
 *
 * Handles:
 *   - Recording subject scores for students
 *   - Score queries and updates
 */
public class ScoreDAO {

    /**
     * Inserts a score record linking a student to a subject with a score.
     *
     * @param studentDbId the DB primary key of the student
     * @param subjectDbId the DB primary key of the subject
     * @param score       the score value (0-100)
     * @return true if inserted successfully
     */
    public boolean insertScore(int studentDbId, int subjectDbId, double score) {
        String sql = "INSERT INTO student_scores (student_id, subject_id, score) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ps.setInt(2, subjectDbId);
            ps.setDouble(3, score);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ScoreDAO.insertScore error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
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
