package tracker.data.dao;

import tracker.data.DBConnectionManager;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Data Access Object for the 'configuration' table.
 *
 * Handles:
 *   - Reading/writing key-value configuration pairs
 *   - Risk weights, thresholds, and other system settings
 */
public class ConfigDAO {

    /**
     * Gets a configuration value by key.
     *
     * @param key the config key
     * @return the value, or null if not found
     */
    public String getValue(String key) {
        String sql = "SELECT config_value FROM configuration WHERE config_key = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, key);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getString("config_value");
        } catch (SQLException e) {
            System.err.println("ConfigDAO.getValue error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return null;
    }

    /**
     * Gets a configuration value as a double, with a default fallback.
     */
    public double getDouble(String key, double defaultValue) {
        String val = getValue(key);
        if (val == null) return defaultValue;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Sets (inserts or updates) a configuration key-value pair.
     */
    public boolean setValue(String key, String value) {
        // Try update first
        String updateSql = "UPDATE configuration SET config_value = ?, updated_at = datetime('now') " +
                           "WHERE config_key = ?";
        String insertSql = "INSERT INTO configuration (config_key, config_value) VALUES (?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(updateSql);
            ps.setString(1, value);
            ps.setString(2, key);
            int updated = ps.executeUpdate();
            if (updated > 0) return true;
            ps.close();

            // Key doesn't exist yet — insert
            ps = conn.prepareStatement(insertSql);
            ps.setString(1, key);
            ps.setString(2, value);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ConfigDAO.setValue error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Returns all configuration entries as an ordered map.
     */
    public Map<String, String> getAll() {
        String sql = "SELECT config_key, config_value FROM configuration ORDER BY config_key";
        Map<String, String> map = new LinkedHashMap<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getString("config_key"), rs.getString("config_value"));
            }
        } catch (SQLException e) {
            System.err.println("ConfigDAO.getAll error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return map;
    }
}
