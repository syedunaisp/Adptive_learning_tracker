package tracker.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Centralized JDBC connection manager for the ALIP platform.
 *
 * Uses SQLite as the embedded database engine — zero external server setup.
 * The database file is stored locally as "alip_data.db" in the working directory.
 *
 * Responsibilities:
 *   - Load JDBC driver
 *   - Create/return connections
 *   - Initialize schema on first run
 *   - Safe close handling
 *
 * All DAO classes obtain connections through this manager.
 */
public class DBConnectionManager {

    private static final String DB_FILE = "alip_data.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_FILE;

    private static boolean initialized = false;

    /**
     * Loads the SQLite JDBC driver. Called once at application startup.
     */
    public static synchronized void initialize() {
        if (initialized) return;
        try {
            Class.forName("org.sqlite.JDBC");
            initialized = true;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "SQLite JDBC driver not found. Ensure sqlite-jdbc JAR is on the classpath.", e);
        }
    }

    /**
     * Returns a new connection to the SQLite database.
     * Caller is responsible for closing the connection.
     *
     * @return a live JDBC Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) initialize();
        Connection conn = DriverManager.getConnection(JDBC_URL);
        // Enable WAL mode for better concurrent read performance
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }
        return conn;
    }

    /**
     * Safely closes a connection, suppressing any exception.
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) { }
        }
    }

    /**
     * Safely closes an AutoCloseable resource.
     */
    public static void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) { }
        }
    }

    private DBConnectionManager() { }
}
