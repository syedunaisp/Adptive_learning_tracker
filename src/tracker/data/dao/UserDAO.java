package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.User;
import tracker.model.UserRole;
import tracker.security.PasswordHasher;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for the 'users' table.
 *
 * Handles:
 *   - Authentication (username + password verification)
 *   - CRUD operations on user accounts
 *   - Role-based queries
 *   - Linking student accounts
 */
public class UserDAO {

    /**
     * Authenticates a user by username and plain-text password.
     * Returns the User object if credentials are valid and account is enabled.
     *
     * @param username      the username
     * @param plainPassword the plain-text password
     * @return the authenticated User, or null if invalid
     */
    public User authenticate(String username, String plainPassword) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.role_id, u.linked_student_id, " +
                     "u.enabled, u.created_at, r.role_name " +
                     "FROM users u JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.username = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                boolean enabled = rs.getInt("enabled") == 1;
                if (!enabled) return null;
                if (!PasswordHasher.verify(plainPassword, storedHash)) return null;
                return mapUser(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return null;
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Finds a user by username (without password check).
     */
    public User findByUsername(String username) {
        String sql = "SELECT u.id, u.username, u.password_hash, u.role_id, u.linked_student_id, " +
                     "u.enabled, u.created_at, r.role_name " +
                     "FROM users u JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.username = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            rs = ps.executeQuery();
            if (rs.next()) return mapUser(rs);
            return null;
        } catch (SQLException e) {
            System.err.println("findByUsername error: " + e.getMessage());
            return null;
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Creates a new user with a hashed password.
     *
     * @param username      the username
     * @param plainPassword the plain-text password (will be hashed)
     * @param role          the user role
     * @param linkedStudentDbId  the DB primary key of linked student (null if not student)
     * @return true if created successfully
     */
    public boolean createUser(String username, String plainPassword, UserRole role, Integer linkedStudentDbId) {
        String sql = "INSERT INTO users (username, password_hash, role_id, linked_student_id, enabled) " +
                     "VALUES (?, ?, ?, ?, 1)";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, username);
            ps.setString(2, PasswordHasher.hash(plainPassword));
            ps.setInt(3, roleToId(role));
            if (linkedStudentDbId != null) ps.setInt(4, linkedStudentDbId);
            else ps.setNull(4, Types.INTEGER);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("createUser error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Returns all users in the system.
     */
    public List<User> findAll() {
        String sql = "SELECT u.id, u.username, u.password_hash, u.role_id, u.linked_student_id, " +
                     "u.enabled, u.created_at, r.role_name " +
                     "FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.id";
        List<User> users = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) users.add(mapUser(rs));
        } catch (SQLException e) {
            System.err.println("findAll users error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return users;
    }

    /**
     * Enables or disables a user account.
     */
    public boolean setEnabled(int userId, boolean enabled) {
        String sql = "UPDATE users SET enabled = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, enabled ? 1 : 0);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("setEnabled error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Updates the linked student for a user.
     */
    public boolean linkStudent(int userId, Integer studentDbId) {
        String sql = "UPDATE users SET linked_student_id = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            if (studentDbId != null) ps.setInt(1, studentDbId);
            else ps.setNull(1, Types.INTEGER);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("linkStudent error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Returns the total number of users.
     */
    public int countAll() {
        String sql = "SELECT COUNT(*) FROM users";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("countAll users error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return 0;
    }

    /**
     * Resolves the linked business student_id for a user with a linked_student_id.
     */
    public String resolveLinkedStudentId(int linkedStudentDbId) {
        String sql = "SELECT student_id FROM students WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, linkedStudentDbId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getString("student_id");
        } catch (SQLException e) {
            System.err.println("resolveLinkedStudentId error: " + e.getMessage());
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
        return null;
    }

    /**
     * Updates a user's password (hashes the new password).
     *
     * @param userId           the DB primary key of the user
     * @param newPlainPassword the new plain-text password
     * @return true if updated successfully
     */
    public boolean updatePassword(int userId, String newPlainPassword) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, PasswordHasher.hash(newPlainPassword));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("updatePassword error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    /**
     * Verifies a user's current password by user ID.
     *
     * @param userId        the DB primary key of the user
     * @param plainPassword the plain-text password to verify
     * @return true if the password matches
     */
    public boolean verifyPassword(int userId, String plainPassword) {
        String sql = "SELECT password_hash FROM users WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnectionManager.getConnection();
            ps = conn.prepareStatement(sql);
            ps.setInt(1, userId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return PasswordHasher.verify(plainPassword, rs.getString("password_hash"));
            }
            return false;
        } catch (SQLException e) {
            System.err.println("verifyPassword error: " + e.getMessage());
            return false;
        } finally {
            DBConnectionManager.closeQuietly(rs);
            DBConnectionManager.closeQuietly(ps);
            DBConnectionManager.closeQuietly(conn);
        }
    }

    // --- Helpers ---

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(roleFromName(rs.getString("role_name")));
        int linkedId = rs.getInt("linked_student_id");
        u.setLinkedStudentDbId(rs.wasNull() ? null : linkedId);
        u.setEnabled(rs.getInt("enabled") == 1);
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    }

    private UserRole roleFromName(String name) {
        if (name == null) return UserRole.STUDENT;
        switch (name.toUpperCase()) {
            case "ADMIN": return UserRole.ADMIN;
            case "TEACHER": return UserRole.TEACHER;
            default: return UserRole.STUDENT;
        }
    }

    private int roleToId(UserRole role) {
        switch (role) {
            case ADMIN: return 1;
            case TEACHER: return 2;
            default: return 3;
        }
    }
}
