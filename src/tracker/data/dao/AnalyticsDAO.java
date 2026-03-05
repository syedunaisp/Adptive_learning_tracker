package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.TopicScore;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for analytics queries (peer benchmarking, institutional
 * dashboard).
 * This DAO is read-only — no INSERT/UPDATE/DELETE operations.
 */
public class AnalyticsDAO {

    /**
     * Returns the overall class average across all students.
     */
    public double getClassAverage() {
        String sql = "SELECT AVG(avg_score) FROM (" +
                "  SELECT student_id, AVG(score) as avg_score " +
                "  FROM student_scores GROUP BY student_id" +
                ")";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("AnalyticsDAO.getClassAverage error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return 0.0;
    }

    /**
     * Returns the average score of the top 10% of students.
     */
    public double getTop10PercentAverage() {
        String sql = "SELECT AVG(avg_score) FROM (" +
                "  SELECT AVG(score) as avg_score FROM student_scores " +
                "  GROUP BY student_id ORDER BY avg_score DESC " +
                "  LIMIT MAX(1, (SELECT COUNT(DISTINCT student_id) FROM student_scores) / 10)" +
                ")";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("AnalyticsDAO.getTop10PercentAverage error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return 0.0;
    }

    /**
     * Returns the percentile rank (0-100) of a specific student.
     */
    public double getStudentPercentile(int studentDbId) {
        String sql = "SELECT COUNT(*) as below_count FROM (" +
                "  SELECT student_id, AVG(score) as avg_score " +
                "  FROM student_scores GROUP BY student_id" +
                ") WHERE avg_score < (" +
                "  SELECT AVG(score) FROM student_scores WHERE student_id = ?" +
                ")";
        String totalSql = "SELECT COUNT(DISTINCT student_id) FROM student_scores";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();

            // Get total student count
            Statement stmt = conn.createStatement();
            ResultSet totalRs = stmt.executeQuery(totalSql);
            int total = 0;
            if (totalRs.next()) {
                total = totalRs.getInt(1);
            }
            totalRs.close();
            stmt.close();

            if (total == 0)
                return 0.0;

            // Get count below this student
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int belowCount = rs.getInt("below_count");
                rs.close();
                return (double) belowCount / total * 100.0;
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("AnalyticsDAO.getStudentPercentile error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return 0.0;
    }

    /**
     * Returns a map of subject name → average score across all students.
     */
    public Map<String, Double> getSubjectAverages() {
        String sql = "SELECT s.subject_name, AVG(ss.score) as avg_score " +
                "FROM student_scores ss " +
                "JOIN subjects s ON ss.subject_id = s.id " +
                "GROUP BY s.subject_name ORDER BY avg_score ASC";
        Map<String, Double> averages = new LinkedHashMap<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                averages.put(rs.getString("subject_name"), rs.getDouble("avg_score"));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("AnalyticsDAO.getSubjectAverages error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return averages;
    }

    /**
     * Returns topic-level scores for a specific student and subject.
     */
    public List<TopicScore> getTopicScoresByStudent(int studentDbId, int subjectDbId) {
        String sql = "SELECT ts.id, ts.score_id, ts.topic_id, ts.score_value, ts.proficiency_level " +
                "FROM topic_scores ts " +
                "JOIN student_scores ss ON ts.score_id = ss.id " +
                "JOIN topics t ON ts.topic_id = t.id " +
                "WHERE ss.student_id = ? AND t.subject_id = ?";
        List<TopicScore> scores = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ps.setInt(2, subjectDbId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                scores.add(new TopicScore(
                        rs.getInt("id"), rs.getInt("score_id"), rs.getInt("topic_id"),
                        rs.getDouble("score_value"), rs.getString("proficiency_level")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("AnalyticsDAO.getTopicScoresByStudent error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return scores;
    }
}
