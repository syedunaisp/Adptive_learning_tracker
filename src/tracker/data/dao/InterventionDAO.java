package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.InterventionLog;
import tracker.model.InterventionPlan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for 'intervention_plans' and 'intervention_logs' tables.
 */
public class InterventionDAO {

    public int insertPlan(int studentDbId, int teacherDbId, String type, String description) {
        String sql = "INSERT INTO intervention_plans (student_id, teacher_id, intervention_type, description) " +
                "VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, studentDbId);
            ps.setInt(2, teacherDbId);
            ps.setString(3, type);
            ps.setString(4, description);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) {
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("InterventionDAO.insertPlan error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return -1;
    }

    public List<InterventionPlan> getPlansByStudent(int studentDbId) {
        String sql = "SELECT id, student_id, teacher_id, intervention_type, description, start_date, status " +
                "FROM intervention_plans WHERE student_id = ? ORDER BY start_date DESC";
        return queryPlans(sql, studentDbId);
    }

    public List<InterventionPlan> getPlansByTeacher(int teacherDbId) {
        String sql = "SELECT id, student_id, teacher_id, intervention_type, description, start_date, status " +
                "FROM intervention_plans WHERE teacher_id = ? ORDER BY start_date DESC";
        return queryPlans(sql, teacherDbId);
    }

    private List<InterventionPlan> queryPlans(String sql, int paramId) {
        List<InterventionPlan> plans = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, paramId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                plans.add(new InterventionPlan(
                        rs.getInt("id"), rs.getInt("student_id"), rs.getInt("teacher_id"),
                        rs.getString("intervention_type"), rs.getString("description"),
                        rs.getString("start_date"), rs.getString("status")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("InterventionDAO.queryPlans error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return plans;
    }

    public boolean updatePlanStatus(int planId, String status) {
        String sql = "UPDATE intervention_plans SET status = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, planId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("InterventionDAO.updatePlanStatus error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public boolean insertLog(int planId, String notes, String outcomeMetric) {
        String sql = "INSERT INTO intervention_logs (plan_id, notes, outcome_metric) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, planId);
            ps.setString(2, notes);
            ps.setString(3, outcomeMetric);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("InterventionDAO.insertLog error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    public List<InterventionLog> getLogsByPlan(int planId) {
        String sql = "SELECT id, plan_id, notes, outcome_metric, log_date " +
                "FROM intervention_logs WHERE plan_id = ? ORDER BY log_date DESC";
        List<InterventionLog> logs = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, planId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                logs.add(new InterventionLog(
                        rs.getInt("id"), rs.getInt("plan_id"),
                        rs.getString("notes"), rs.getString("outcome_metric"),
                        rs.getString("log_date")));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("InterventionDAO.getLogsByPlan error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return logs;
    }
}
