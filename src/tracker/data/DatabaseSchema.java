package tracker.data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database schema creation and seeding for the ALIP platform.
 *
 * Tables:
 *   - roles              (id, role_name)
 *   - users              (id, username, password_hash, role_id, linked_student_id, enabled, created_at)
 *   - students           (id, student_id, name, created_at)
 *   - subjects           (id, subject_name, category)
 *   - student_scores     (id, student_id, subject_id, score, recorded_at)
 *   - configuration      (id, config_key, config_value, updated_at)
 *
 * All tables use INTEGER PRIMARY KEY AUTOINCREMENT (SQLite).
 * Foreign keys are enforced via PRAGMA foreign_keys=ON in DBConnectionManager.
 */
public class DatabaseSchema {

    /**
     * Creates all tables if they do not exist, then seeds default data.
     */
    public static void initializeSchema() {
        Connection conn = null;
        try {
            conn = DBConnectionManager.getConnection();
            createTables(conn);
            seedDefaults(conn);
        } catch (SQLException e) {
            System.err.println("Schema initialization failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DBConnectionManager.closeQuietly(conn);
        }
    }

    private static void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // --- roles ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS roles (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  role_name TEXT NOT NULL UNIQUE" +
                ")"
            );

            // --- users ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username TEXT NOT NULL UNIQUE," +
                "  password_hash TEXT NOT NULL," +
                "  role_id INTEGER NOT NULL," +
                "  linked_student_id INTEGER," +
                "  enabled INTEGER NOT NULL DEFAULT 1," +
                "  created_at TEXT NOT NULL DEFAULT (datetime('now'))," +
                "  FOREIGN KEY (role_id) REFERENCES roles(id)," +
                "  FOREIGN KEY (linked_student_id) REFERENCES students(id)" +
                ")"
            );

            // --- students ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS students (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  student_id TEXT NOT NULL UNIQUE," +
                "  name TEXT NOT NULL," +
                "  created_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );

            // --- subjects ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS subjects (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  subject_name TEXT NOT NULL UNIQUE," +
                "  category TEXT NOT NULL DEFAULT 'UNCATEGORIZED'" +
                ")"
            );

            // --- student_scores ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS student_scores (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  student_id INTEGER NOT NULL," +
                "  subject_id INTEGER NOT NULL," +
                "  score REAL NOT NULL," +
                "  recorded_at TEXT NOT NULL DEFAULT (datetime('now'))," +
                "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE" +
                ")"
            );

            // --- configuration ---
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS configuration (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  config_key TEXT NOT NULL UNIQUE," +
                "  config_value TEXT NOT NULL," +
                "  updated_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                ")"
            );

            // --- indexes ---
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_students_student_id ON students(student_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_student_scores_student ON student_scores(student_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_student_scores_subject ON student_scores(subject_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_configuration_key ON configuration(config_key)");
        }
    }

    /**
     * Seeds default roles and a default admin user if they don't exist.
     * Default admin credentials: admin / admin123
     */
    private static void seedDefaults(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Insert roles
            stmt.execute("INSERT OR IGNORE INTO roles (id, role_name) VALUES (1, 'ADMIN')");
            stmt.execute("INSERT OR IGNORE INTO roles (id, role_name) VALUES (2, 'TEACHER')");
            stmt.execute("INSERT OR IGNORE INTO roles (id, role_name) VALUES (3, 'STUDENT')");

            // Check if admin user exists
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Seed default admin — password: admin123
                String hash = tracker.security.PasswordHasher.hash("admin123");
                stmt.execute("INSERT INTO users (username, password_hash, role_id, enabled) " +
                             "VALUES ('admin', '" + hash + "', 1, 1)");
            }
            rs.close();

            // Seed default teacher — password: teacher123
            rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'teacher'");
            if (rs.next() && rs.getInt(1) == 0) {
                String hash = tracker.security.PasswordHasher.hash("teacher123");
                stmt.execute("INSERT INTO users (username, password_hash, role_id, enabled) " +
                             "VALUES ('teacher', '" + hash + "', 2, 1)");
            }
            rs.close();

            // Seed default risk weight configs
            stmt.execute("INSERT OR IGNORE INTO configuration (config_key, config_value) " +
                         "VALUES ('risk.weight.average', '0.35')");
            stmt.execute("INSERT OR IGNORE INTO configuration (config_key, config_value) " +
                         "VALUES ('risk.weight.weak_count', '0.25')");
            stmt.execute("INSERT OR IGNORE INTO configuration (config_key, config_value) " +
                         "VALUES ('risk.weight.lowest', '0.25')");
            stmt.execute("INSERT OR IGNORE INTO configuration (config_key, config_value) " +
                         "VALUES ('risk.weight.trend', '0.15')");
            stmt.execute("INSERT OR IGNORE INTO configuration (config_key, config_value) " +
                         "VALUES ('risk.threshold.high', '60.0')");
            stmt.execute("INSERT OR IGNORE INTO configuration (config_key, config_value) " +
                         "VALUES ('risk.threshold.moderate', '35.0')");
        }
    }

    private DatabaseSchema() { }
}
