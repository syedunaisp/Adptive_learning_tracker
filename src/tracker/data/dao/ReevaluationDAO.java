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

    public boolean submitRequest(int scoreId, int studentDbId, String reason) {
        String sql = "INSERT INTO reevaluation_requests (score_id, student_id, reason) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, scoreId);
            ps.setInt(2, studentDbId);
            ps.setString(3, reason);
            return ps.executeUpdate() > 0;
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
                "resolved_by, resolution_notes, resolved_at " +
                "FROM reevaluation_requests WHERE student_id = ? ORDER BY submitted_at DESC";
        return queryRequests(sql, studentDbId);
    }

    public List<ReevaluationRequest> getPendingRequests() {
        String sql = "SELECT id, score_id, student_id, reason, status, submitted_at, " +
                "resolved_by, resolution_notes, resolved_at " +
                "FROM reevaluation_requests WHERE status = 'PENDING' ORDER BY submitted_at ASC";
        List<ReevaluationRequest> requests = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                requests.add(mapRequest(rs));
            }
            rs.close();
        } catch (SQLException e) {
            System.err.println("ReevaluationDAO.getPendingRequests error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return requests;
    }

    public boolean resolveRequest(int requestId, int resolvedByUserId, String notes) {
        return updateRequestStatus(requestId, "RESOLVED", resolvedByUserId, notes);
    }

    public boolean rejectRequest(int requestId, int resolvedByUserId, String notes) {
        return updateRequestStatus(requestId, "REJECTED", resolvedByUserId, notes);
    }

    private boolean updateRequestStatus(int requestId, String status, int resolvedByUserId, String notes) {
        String sql = "UPDATE reevaluation_requests SET status = ?, resolved_by = ?, " +
                "resolution_notes = ?, resolved_at = datetime('now') WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, status);
            ps.setInt(2, resolvedByUserId);
            ps.setString(3, notes);
            ps.setInt(4, requestId);
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
        return new ReevaluationRequest(
                rs.getInt("id"), rs.getInt("score_id"), rs.getInt("student_id"),
                rs.getString("reason"), rs.getString("status"), rs.getString("submitted_at"),
                resolvedBy, rs.getString("resolution_notes"), rs.getString("resolved_at"));
    }
}
