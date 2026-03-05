package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.Goal;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'student_goals' table.
 */
public class GoalDAO {

    public boolean insertGoal(int studentDbId, Integer subjectDbId, double targetScore, String deadline) {
        String sql = "INSERT INTO student_goals (student_id, subject_id, target_score, deadline) VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            if (subjectDbId != null) {
                ps.setInt(2, subjectDbId);
            } else {
                ps.setNull(2, Types.INTEGER);
            }
            ps.setDouble(3, targetScore);
            ps.setString(4, deadline);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("GoalDAO.insertGoal error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public List<Goal> getGoalsByStudent(int studentDbId) {
        String sql = "SELECT id, student_id, subject_id, target_score, deadline, status, created_at " +
                "FROM student_goals WHERE student_id = ? ORDER BY created_at DESC";
        List<Goal> goals = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, studentDbId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int subjId = rs.getInt("subject_id");
                Integer subjectId = rs.wasNull() ? null : subjId;
                goals.add(new Goal(
                        rs.getInt("id"), rs.getInt("student_id"), subjectId,
                        rs.getDouble("target_score"), rs.getString("deadline"),
                        rs.getString("status"), rs.getString("created_at")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("GoalDAO.getGoalsByStudent error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return goals;
    }

    public boolean updateGoalStatus(int goalId, String status) {
        String sql = "UPDATE student_goals SET status = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, goalId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("GoalDAO.updateGoalStatus error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public boolean deleteGoal(int goalId) {
        String sql = "DELETE FROM student_goals WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, goalId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("GoalDAO.deleteGoal error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }
}
