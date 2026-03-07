package tracker.data.dao;

import tracker.data.DBConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for student study strategies.
 * Handles persisting and retrieving questionnaire results.
 */
public class StudyStrategyDAO {

    /**
     * Saves a new study strategy result for a student.
     */
    public boolean saveStrategy(int studentDbId, String answersJson, String recommendedTechniques) {
        String sql = "INSERT INTO student_study_strategy (student_id, answers_json, recommended_techniques) VALUES (?, ?, ?)";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentDbId);
            pstmt.setString(2, answersJson);
            pstmt.setString(3, recommendedTechniques);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error saving study strategy: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retrieves the latest recommended techniques for a student.
     * 
     * @return A string containing the recommended techniques, or null if none
     *         exist.
     */
    public String getLatestTechniques(int studentDbId) {
        String sql = "SELECT recommended_techniques FROM student_study_strategy WHERE student_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, studentDbId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("recommended_techniques");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving study strategy: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Return null if not found
    }
}
