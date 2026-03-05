package tracker;

/**
 * Minimal verification utility for Phase 1 schema.
 * Creates the database and prints table info to stdout.
 */
public class SchemaVerifier {
    public static void main(String[] args) {
        try {
            // Initialize the schema (creates DB + tables + seeds)
            tracker.data.DatabaseSchema.initializeSchema();
            System.out.println("[OK] Schema initialized successfully.");

            // Verify tables exist
            java.sql.Connection conn = tracker.data.DBConnectionManager.getConnection();
            java.sql.Statement stmt = conn.createStatement();

            // Check user_version
            java.sql.ResultSet rs = stmt.executeQuery("PRAGMA user_version");
            if (rs.next()) {
                System.out.println("[OK] Schema version: " + rs.getInt(1));
            }
            rs.close();

            // Count tables
            rs = stmt
                    .executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'");
            if (rs.next()) {
                System.out.println("[OK] Total tables: " + rs.getInt(1));
            }
            rs.close();

            // List all tables
            rs = stmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
            System.out.println("[OK] Tables:");
            while (rs.next()) {
                System.out.println("     - " + rs.getString(1));
            }
            rs.close();

            // Check learning profiles seed data
            rs = stmt.executeQuery("SELECT COUNT(*) FROM learning_profiles");
            if (rs.next()) {
                System.out.println("[OK] Learning profiles count: " + rs.getInt(1));
            }
            rs.close();

            // Check study strategies seed data
            rs = stmt.executeQuery("SELECT COUNT(*) FROM study_strategies");
            if (rs.next()) {
                System.out.println("[OK] Study strategies count: " + rs.getInt(1));
            }
            rs.close();

            // Check questionnaire questions
            rs = stmt.executeQuery("SELECT COUNT(*) FROM questionnaire_questions");
            if (rs.next()) {
                System.out.println("[OK] Questionnaire questions count: " + rs.getInt(1));
            }
            rs.close();

            stmt.close();
            tracker.data.DBConnectionManager.closeQuietly(conn);
            System.out.println("\n=== Phase 1 Verification PASSED ===");

        } catch (Exception e) {
            System.err.println("[FAIL] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
