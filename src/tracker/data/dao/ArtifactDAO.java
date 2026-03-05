package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.ExamArtifact;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'exam_artifacts' table.
 */
public class ArtifactDAO {

    public boolean insertArtifact(int scoreId, int teacherDbId, String filePath,
            String originalFilename, String feedback) {
        String sql = "INSERT INTO exam_artifacts (score_id, teacher_id, file_path, original_filename, feedback_text) " +
                "VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, scoreId);
            ps.setInt(2, teacherDbId);
            ps.setString(3, filePath);
            ps.setString(4, originalFilename);
            ps.setString(5, feedback);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ArtifactDAO.insertArtifact error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public List<ExamArtifact> getArtifactsByScore(int scoreId) {
        String sql = "SELECT id, score_id, teacher_id, file_path, original_filename, upload_date, feedback_text " +
                "FROM exam_artifacts WHERE score_id = ? ORDER BY upload_date DESC";
        return queryArtifacts(sql, scoreId);
    }

    public List<ExamArtifact> getArtifactsByStudent(int studentDbId) {
        String sql = "SELECT ea.id, ea.score_id, ea.teacher_id, ea.file_path, ea.original_filename, " +
                "ea.upload_date, ea.feedback_text " +
                "FROM exam_artifacts ea " +
                "JOIN student_scores ss ON ea.score_id = ss.id " +
                "WHERE ss.student_id = ? ORDER BY ea.upload_date DESC";
        return queryArtifacts(sql, studentDbId);
    }

    private List<ExamArtifact> queryArtifacts(String sql, int paramId) {
        List<ExamArtifact> artifacts = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, paramId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                artifacts.add(new ExamArtifact(
                        rs.getInt("id"), rs.getInt("score_id"), rs.getInt("teacher_id"),
                        rs.getString("file_path"), rs.getString("original_filename"),
                        rs.getString("upload_date"), rs.getString("feedback_text")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ArtifactDAO.queryArtifacts error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return artifacts;
    }

    public boolean deleteArtifact(int artifactId) {
        String sql = "DELETE FROM exam_artifacts WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, artifactId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ArtifactDAO.deleteArtifact error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }
}
