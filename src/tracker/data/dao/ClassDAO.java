package tracker.data.dao;

import tracker.data.DBConnectionManager;
import tracker.model.ClassRoom;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClassDAO {

    public int insert(String className, String section) {
        return insert(className, section, 0);
    }

    public int insert(String className, String section, int teacherId) {
        String sql = "INSERT INTO classes (class_name, section, teacher_id) VALUES (?, ?, ?)";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, className);
            ps.setString(2, section);
            if (teacherId > 0) {
                ps.setInt(3, teacherId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("ClassDAO.insert error: " + e.getMessage());
        }
        return -1;
    }

    public List<ClassRoom> loadAll() {
        String sql = "SELECT id, class_name, section FROM classes ORDER BY class_name, section";
        List<ClassRoom> classes = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                classes.add(new ClassRoom(
                        rs.getInt("id"),
                        rs.getString("class_name"),
                        rs.getString("section")));
            }
        } catch (SQLException e) {
            System.err.println("ClassDAO.loadAll error: " + e.getMessage());
        }
        return classes;
    }

    public List<ClassRoom> loadByTeacherId(int teacherId) {
        String sql = "SELECT id, class_name, section FROM classes WHERE teacher_id = ? ORDER BY class_name, section";
        List<ClassRoom> classes = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classes.add(new ClassRoom(
                            rs.getInt("id"),
                            rs.getString("class_name"),
                            rs.getString("section")));
                }
            }
        } catch (SQLException e) {
            System.err.println("ClassDAO.loadByTeacherId error: " + e.getMessage());
        }
        return classes;
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM classes WHERE id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("ClassDAO.deleteById error: " + e.getMessage());
            return false;
        }
    }
}
