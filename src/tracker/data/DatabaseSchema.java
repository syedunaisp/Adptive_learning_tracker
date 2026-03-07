package tracker.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages database schema creation, versioning, and seeding for the ALIP
 * platform.
 *
 * V1 Tables (original):
 * - roles (id, role_name)
 * - users (id, username, password_hash, role_id, linked_student_id, enabled,
 * created_at)
 * - students (id, student_id, name, created_at)
 * - subjects (id, subject_name, category)
 * - student_scores (id, student_id, subject_id, score, recorded_at)
 * - configuration (id, config_key, config_value, updated_at)
 *
 * V2 Tables (new features):
 * - learning_profiles, study_strategies, questionnaire_questions
 * - student_questionnaire_responses, student_learning_profiles
 * - topics, topic_scores
 * - student_goals
 * - intervention_plans, intervention_logs
 * - exam_artifacts
 * - reevaluation_requests
 *
 * All tables use INTEGER PRIMARY KEY AUTOINCREMENT (SQLite).
 * Foreign keys are enforced via PRAGMA foreign_keys=ON in DBConnectionManager.
 * Schema versioning uses PRAGMA user_version.
 */
public class DatabaseSchema {

        /** Current schema version. Increment when adding new migrations. */
        private static final int CURRENT_SCHEMA_VERSION = 2;

        /**
         * Creates all tables if they do not exist, seeds default data,
         * then runs any pending schema migrations.
         */
        public static void initializeSchema() {
                Connection conn = null;
                try {
                        conn = DBConnectionManager.getConnection();
                        createTables(conn);
                        seedDefaults(conn);
                        migrateSchema(conn);
                } catch (SQLException e) {
                        System.err.println("Schema initialization failed: " + e.getMessage());
                        e.printStackTrace();
                } finally {
                        DBConnectionManager.closeQuietly(conn);
                }
        }

        /**
         * Checks the current schema version and applies any pending migrations.
         */
        private static void migrateSchema(Connection conn) throws SQLException {
                int oldVersion = getSchemaVersion(conn);

                if (oldVersion < 2) {
                        System.out.println("[Schema] Migrating from version " + oldVersion + " to 2...");
                        createV2Tables(conn);
                        seedV2Defaults(conn);
                        setSchemaVersion(conn, 2);
                        System.out.println("[Schema] Migration to version 2 complete.");
                }

                if (oldVersion < 3) {
                        System.out.println("[Schema] Migrating from version " + oldVersion + " to 3...");
                        createV3Tables(conn);
                        setSchemaVersion(conn, 3);
                        System.out.println("[Schema] Migration to version 3 complete.");
                }

                if (oldVersion < 4) {
                        System.out.println("[Schema] Migrating from version " + oldVersion + " to 4...");
                        migrateV4(conn);
                        setSchemaVersion(conn, 4);
                        System.out.println("[Schema] Migration to version 4 complete.");
                }

                if (oldVersion < 5) {
                        System.out.println("[Schema] Migrating from version " + oldVersion + " to 5...");
                        migrateV5(conn);
                        setSchemaVersion(conn, 5);
                        System.out.println("[Schema] Migration to version 5 complete.");
                }
        }

        private static int getSchemaVersion(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement();
                                ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
                        if (rs.next()) {
                                return rs.getInt(1);
                        }
                }
                return 0;
        }

        private static void setSchemaVersion(Connection conn, int version) throws SQLException {
                try (Statement stmt = conn.createStatement()) {
                        stmt.execute("PRAGMA user_version = " + version);
                }
        }

        // =========================================================================
        // V1 Tables (original)
        // =========================================================================

        private static void createTables(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {

                        // --- roles ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS roles (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  role_name TEXT NOT NULL UNIQUE" +
                                                        ")");

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
                                                        ")");

                        // --- students ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS students (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  student_id TEXT NOT NULL UNIQUE," +
                                                        "  name TEXT NOT NULL," +
                                                        "  created_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                                                        ")");

                        // --- subjects ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS subjects (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  subject_name TEXT NOT NULL UNIQUE," +
                                                        "  category TEXT NOT NULL DEFAULT 'UNCATEGORIZED'" +
                                                        ")");

                        // --- student_scores ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS student_scores (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  student_id INTEGER NOT NULL," +
                                                        "  subject_id INTEGER NOT NULL," +
                                                        "  score REAL NOT NULL," +
                                                        "  recorded_at TEXT NOT NULL DEFAULT (datetime('now'))," +
                                                        "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE"
                                                        +
                                                        ")");

                        // --- configuration ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS configuration (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  config_key TEXT NOT NULL UNIQUE," +
                                                        "  config_value TEXT NOT NULL," +
                                                        "  updated_at TEXT NOT NULL DEFAULT (datetime('now'))" +
                                                        ")");

                        // --- v1 indexes ---
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_students_student_id ON students(student_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_student_scores_student ON student_scores(student_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_student_scores_subject ON student_scores(subject_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_configuration_key ON configuration(config_key)");
                }
        }

        // =========================================================================
        // V2 Tables (new features)
        // =========================================================================

        private static void createV2Tables(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {

                        // --- learning_profiles ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS learning_profiles (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  profile_name TEXT NOT NULL UNIQUE," +
                                                        "  description TEXT" +
                                                        ")");

                        // --- study_strategies ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS study_strategies (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  profile_id INTEGER NOT NULL," +
                                                        "  strategy_name TEXT NOT NULL," +
                                                        "  instructions TEXT," +
                                                        "  FOREIGN KEY (profile_id) REFERENCES learning_profiles(id)," +
                                                        "  UNIQUE(profile_id, strategy_name)" +
                                                        ")");

                        // --- questionnaire_questions ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS questionnaire_questions (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  question_text TEXT NOT NULL," +
                                                        "  option_a TEXT," +
                                                        "  option_b TEXT," +
                                                        "  option_c TEXT," +
                                                        "  option_d TEXT" +
                                                        ")");

                        // --- student_questionnaire_responses ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS student_questionnaire_responses (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  student_id INTEGER NOT NULL," +
                                                        "  question_id INTEGER NOT NULL," +
                                                        "  answer_value TEXT NOT NULL," +
                                                        "  responded_at TEXT DEFAULT (datetime('now'))," +
                                                        "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (question_id) REFERENCES questionnaire_questions(id)"
                                                        +
                                                        ")");

                        // --- student_learning_profiles ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS student_learning_profiles (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  student_id INTEGER NOT NULL," +
                                                        "  profile_id INTEGER NOT NULL," +
                                                        "  assigned_date TEXT DEFAULT (datetime('now'))," +
                                                        "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (profile_id) REFERENCES learning_profiles(id)," +
                                                        "  UNIQUE(student_id)" +
                                                        ")");

                        // --- topics ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS topics (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  subject_id INTEGER NOT NULL," +
                                                        "  topic_name TEXT NOT NULL," +
                                                        "  FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,"
                                                        +
                                                        "  UNIQUE(subject_id, topic_name)" +
                                                        ")");

                        // --- topic_scores ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS topic_scores (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  score_id INTEGER NOT NULL," +
                                                        "  topic_id INTEGER NOT NULL," +
                                                        "  score_value REAL NOT NULL," +
                                                        "  proficiency_level TEXT DEFAULT 'UNKNOWN'," +
                                                        "  FOREIGN KEY (score_id) REFERENCES student_scores(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (topic_id) REFERENCES topics(id) ON DELETE CASCADE"
                                                        +
                                                        ")");

                        // --- student_goals ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS student_goals (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  student_id INTEGER NOT NULL," +
                                                        "  subject_id INTEGER," +
                                                        "  target_score REAL NOT NULL," +
                                                        "  deadline TEXT," +
                                                        "  status TEXT NOT NULL DEFAULT 'ACTIVE'," +
                                                        "  created_at TEXT DEFAULT (datetime('now'))," +
                                                        "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (subject_id) REFERENCES subjects(id)" +
                                                        ")");

                        // --- intervention_plans ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS intervention_plans (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  student_id INTEGER NOT NULL," +
                                                        "  teacher_id INTEGER NOT NULL," +
                                                        "  intervention_type TEXT NOT NULL," +
                                                        "  description TEXT," +
                                                        "  start_date TEXT DEFAULT (datetime('now'))," +
                                                        "  status TEXT NOT NULL DEFAULT 'ACTIVE'," +
                                                        "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (teacher_id) REFERENCES users(id)" +
                                                        ")");

                        // --- intervention_logs ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS intervention_logs (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  plan_id INTEGER NOT NULL," +
                                                        "  notes TEXT," +
                                                        "  outcome_metric TEXT," +
                                                        "  log_date TEXT DEFAULT (datetime('now'))," +
                                                        "  FOREIGN KEY (plan_id) REFERENCES intervention_plans(id) ON DELETE CASCADE"
                                                        +
                                                        ")");

                        // --- exam_artifacts ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS exam_artifacts (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  score_id INTEGER," +
                                                        "  teacher_id INTEGER," +
                                                        "  file_path TEXT NOT NULL," +
                                                        "  original_filename TEXT," +
                                                        "  upload_date TEXT DEFAULT (datetime('now'))," +
                                                        "  feedback_text TEXT," +
                                                        "  FOREIGN KEY (score_id) REFERENCES student_scores(id) ON DELETE SET NULL,"
                                                        +
                                                        "  FOREIGN KEY (teacher_id) REFERENCES users(id)" +
                                                        ")");

                        // --- reevaluation_requests ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS reevaluation_requests (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  score_id INTEGER NOT NULL," +
                                                        "  student_id INTEGER NOT NULL," +
                                                        "  reason TEXT NOT NULL," +
                                                        "  status TEXT NOT NULL DEFAULT 'PENDING'," +
                                                        "  submitted_at TEXT DEFAULT (datetime('now'))," +
                                                        "  resolved_by INTEGER," +
                                                        "  resolution_notes TEXT," +
                                                        "  resolved_at TEXT," +
                                                        "  FOREIGN KEY (score_id) REFERENCES student_scores(id)," +
                                                        "  FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,"
                                                        +
                                                        "  FOREIGN KEY (resolved_by) REFERENCES users(id)" +
                                                        ")");

                        // --- v2 indexes ---
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_topic_scores_score ON topic_scores(score_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_intervention_plans_student ON intervention_plans(student_id)");
                        stmt.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_reevaluation_requests_student ON reevaluation_requests(student_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_student_goals_student ON student_goals(student_id)");
                        stmt.execute(
                                        "CREATE INDEX IF NOT EXISTS idx_student_learning_profiles_student ON student_learning_profiles(student_id)");
                        stmt.execute("CREATE INDEX IF NOT EXISTS idx_exam_artifacts_score ON exam_artifacts(score_id)");
                }
        }

        private static void createV3Tables(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {
                        // --- classes (Phase 6) ---
                        stmt.execute(
                                        "CREATE TABLE IF NOT EXISTS classes (" +
                                                        "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                                        "  class_name TEXT NOT NULL," +
                                                        "  section TEXT NOT NULL," +
                                                        "  created_at TEXT DEFAULT (datetime('now'))" +
                                                        ")");

                        // --- alter students table fields (Phase 6) ---
                        try {
                                stmt.execute("ALTER TABLE students ADD COLUMN class_id INTEGER REFERENCES classes(id) ON DELETE SET NULL");
                        } catch (SQLException e) {
                        }
                        try {
                                stmt.execute("ALTER TABLE students ADD COLUMN roll_number TEXT");
                        } catch (SQLException e) {
                        }
                        try {
                                stmt.execute("ALTER TABLE students ADD COLUMN email TEXT");
                        } catch (SQLException e) {
                        }
                }
        }

        private static void migrateV4(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE classes ADD COLUMN teacher_id INTEGER REFERENCES users(id)");
                } catch (SQLException e) {
                        // Column may already exist
                }
        }

        private static void migrateV5(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {
                        try {
                                stmt.execute("ALTER TABLE reevaluation_requests ADD COLUMN subject_name TEXT");
                        } catch (SQLException e) {
                                /* already exists */ }
                        try {
                                stmt.execute("ALTER TABLE reevaluation_requests ADD COLUMN teacher_id INTEGER REFERENCES users(id)");
                        } catch (SQLException e) {
                                /* already exists */ }
                        try {
                                stmt.execute("ALTER TABLE reevaluation_requests ADD COLUMN updated_marks REAL");
                        } catch (SQLException e) {
                                /* already exists */ }
                }
        }

        // =========================================================================
        // Seed data
        // =========================================================================

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

        /**
         * Seeds default data for V2 tables: learning profiles, strategies, and
         * questionnaire questions.
         */
        private static void seedV2Defaults(Connection conn) throws SQLException {
                try (Statement stmt = conn.createStatement()) {

                        // --- Learning Profiles ---
                        stmt.execute("INSERT OR IGNORE INTO learning_profiles (id, profile_name, description) " +
                                        "VALUES (1, 'Memorization-Heavy', 'Learns best through repetition and rote memorization techniques')");
                        stmt.execute("INSERT OR IGNORE INTO learning_profiles (id, profile_name, description) " +
                                        "VALUES (2, 'Conceptual', 'Learns best by understanding underlying concepts and principles')");
                        stmt.execute("INSERT OR IGNORE INTO learning_profiles (id, profile_name, description) " +
                                        "VALUES (3, 'Practice-Based', 'Learns best through hands-on exercises and problem solving')");
                        stmt.execute("INSERT OR IGNORE INTO learning_profiles (id, profile_name, description) " +
                                        "VALUES (4, 'Visual', 'Learns best through diagrams, charts, and visual representations')");
                        stmt.execute("INSERT OR IGNORE INTO learning_profiles (id, profile_name, description) " +
                                        "VALUES (5, 'Mixed', 'Benefits from a combination of multiple learning strategies')");

                        // --- Study Strategies per Profile ---
                        // Memorization-Heavy
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (1, 'Spaced Repetition', 'Review material at increasing intervals: 1 day, 3 days, 7 days, 14 days')");
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (1, 'Flashcard Drilling', 'Create flashcards for key facts and test yourself daily')");

                        // Conceptual
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (2, 'Feynman Technique', 'Explain the concept in simple terms as if teaching someone else')");
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (2, 'Concept Mapping', 'Draw diagrams connecting related concepts to see the big picture')");

                        // Practice-Based
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (3, 'Retrieval Practice', 'Test yourself without looking at notes, then check answers')");
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (3, 'Problem Sets', 'Work through progressively harder exercises on each topic')");

                        // Visual
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (4, 'Mind Mapping', 'Create visual mind maps branching from main topics to subtopics')");
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (4, 'Diagram Annotation', 'Annotate diagrams and charts with your own notes and labels')");

                        // Mixed
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (5, 'Active Recall', 'Alternate between reading, summarizing, and self-testing')");
                        stmt.execute("INSERT OR IGNORE INTO study_strategies (profile_id, strategy_name, instructions) "
                                        +
                                        "VALUES (5, 'Interleaved Practice', 'Mix different topics and problem types in one study session')");

                        // --- Questionnaire Questions ---
                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (1, 'How do you usually prepare for an exam?', "
                                                        +
                                                        "'I re-read my notes and textbook.', 'I try to understand the underlying concepts.', "
                                                        +
                                                        "'I do practice tests and problems.', 'I look for diagrams or videos.')");

                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (2, 'How often do you review your notes after a lecture?', "
                                                        +
                                                        "'Rarely, usually only before exams.', 'I try to connect them to what I already know.', "
                                                        +
                                                        "'I rewrite them into study guides and test myself.', 'I highlight them in different colors.')");

                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (3, 'When studying a difficult concept, what is your first step?', "
                                                        +
                                                        "'I read it again and again.', 'I break it down into fundamental principles.', "
                                                        +
                                                        "'I try to find an example problem to solve.', 'I draw a flowchart or mind map.')");

                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (4, 'How do you manage your study time during a long session?', "
                                                        +
                                                        "'I study for hours until I''m done reviewing.', 'I take breaks after grasping a major concept.', "
                                                        +
                                                        "'I time myself solving a set number of problems.', 'I switch between text and visual materials.')");

                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (5, 'What happens when you test yourself on the material?', "
                                                        +
                                                        "'I rarely test myself; I just read.', 'I try to explain it out loud.', "
                                                        +
                                                        "'I score myself on a practice exam.', 'I try to recreate my notes from memory visually.')");

                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (6, 'How do you organize information that needs to be memorized?', "
                                                        +
                                                        "'I write it down repeatedly.', 'I try to understand how concepts connect to each other.', "
                                                        +
                                                        "'I make flashcards and drill them.', 'I use color-coding and diagrams.')");

                        stmt.execute(
                                        "INSERT OR REPLACE INTO questionnaire_questions (id, question_text, option_a, option_b, option_c, option_d) "
                                                        +
                                                        "VALUES (7, 'How do you handle feeling overwhelmed by a large syllabus?', "
                                                        +
                                                        "'I make a detailed schedule but struggle to stick to it.', 'I try to step back and find the main narrative of the course.', "
                                                        +
                                                        "'I divide it into small daily practice goals.', 'I map out the entire syllabus visually on one page.')");
                }
        }

        private DatabaseSchema() {
        }
}
