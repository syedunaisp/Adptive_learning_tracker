package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.LearningProfile;
import tracker.model.StudyStrategy;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for learning profiles, study strategies, and questionnaire
 * data.
 */
public class ProfileDAO {

    public List<LearningProfile> getAllProfiles() {
        String sql = "SELECT id, profile_name, description FROM learning_profiles ORDER BY id";
        List<LearningProfile> profiles = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                profiles.add(new LearningProfile(
                        rs.getInt("id"), rs.getString("profile_name"), rs.getString("description")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ProfileDAO.getAllProfiles error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return profiles;
    }

    public List<StudyStrategy> getStrategiesByProfile(int profileId) {
        String sql = "SELECT id, profile_id, strategy_name, instructions " +
                "FROM study_strategies WHERE profile_id = ?";
        List<StudyStrategy> strategies = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, profileId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                strategies.add(new StudyStrategy(
                        rs.getInt("id"), rs.getInt("profile_id"),
                        rs.getString("strategy_name"), rs.getString("instructions")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ProfileDAO.getStrategiesByProfile error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return strategies;
    }

    public boolean saveStudentProfile(int studentDbId, int profileId) {
        // UPSERT: replace if student already has a profile
        String sql = "INSERT OR REPLACE INTO student_learning_profiles (student_id, profile_id, assigned_date) " +
                "VALUES (?, ?, datetime('now'))";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ps.setInt(2, profileId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ProfileDAO.saveStudentProfile error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public LearningProfile getStudentProfile(int studentDbId) {
        String sql = "SELECT lp.id, lp.profile_name, lp.description " +
                "FROM learning_profiles lp " +
                "JOIN student_learning_profiles slp ON lp.id = slp.profile_id " +
                "WHERE slp.student_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                LearningProfile profile = new LearningProfile(
                        rs.getInt("id"), rs.getString("profile_name"), rs.getString("description"));
                rs.close();
                return profile;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ProfileDAO.getStudentProfile error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return null;
    }

    public boolean saveQuestionnaireResponse(int studentDbId, int questionId, String answer) {
        String sql = "INSERT INTO student_questionnaire_responses (student_id, question_id, answer_value) " +
                "VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ps.setInt(2, questionId);
            ps.setString(3, answer);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ProfileDAO.saveQuestionnaireResponse error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Returns all questionnaire questions as a list of String arrays:
     * [id, question_text, option_a, option_b, option_c, option_d]
     */
    public List<String[]> getQuestionnaireQuestions() {
        String sql = "SELECT id, question_text, option_a, option_b, option_c, option_d " +
                "FROM questionnaire_questions ORDER BY id";
        List<String[]> questions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                questions.add(new String[] {
                        String.valueOf(rs.getInt("id")),
                        rs.getString("question_text"),
                        rs.getString("option_a"),
                        rs.getString("option_b"),
                        rs.getString("option_c"),
                        rs.getString("option_d")
                });
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ProfileDAO.getQuestionnaireQuestions error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return questions;
    }
}
