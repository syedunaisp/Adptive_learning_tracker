---
phase: 1
plan: 1
wave: 1
---

# Plan 1.1: Schema Versioning + New Table DDLs

## Objective
Add a schema versioning mechanism using SQLite `PRAGMA user_version` to safely migrate existing databases. Then add all new tables required by the 12 new features (topics, questionnaire data, goals, interventions, artifacts, re-evaluation requests). This must not break existing tables or data.

## Context
- .gsd/SPEC.md
- .gsd/ARCHITECTURE.md
- src/tracker/data/DatabaseSchema.java
- src/tracker/data/DBConnectionManager.java

## Tasks

<task type="auto">
  <name>Add schema versioning to DatabaseSchema.java</name>
  <files>src/tracker/data/DatabaseSchema.java</files>
  <action>
    Add a private static final int CURRENT_SCHEMA_VERSION = 2 constant.
    In initializeSchema(), after createTables() and seedDefaults(), call a new method migrateSchema(conn).
    migrateSchema(conn) should:
      1. Read current version: "PRAGMA user_version" → int oldVersion
      2. If oldVersion < 2, call createV2Tables(conn) then execute "PRAGMA user_version = 2"
      3. Log a message: "Schema migrated from version X to Y"
    This ensures existing v1 databases get the new tables without losing data.
    DO NOT modify any existing CREATE TABLE statements — only add new ones.
  </action>
  <verify>
    Compile: javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/data/DatabaseSchema.java
    Run: java -cp "bin;lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar;lib/slf4j-nop-2.0.9.jar" -e "tracker.data.DatabaseSchema.initializeSchema()"
    Check: sqlite3 alip_data.db "PRAGMA user_version;" returns 2
  </verify>
  <done>DatabaseSchema.java compiles. PRAGMA user_version is set to 2 after initialization. No existing tables are modified.</done>
</task>

<task type="auto">
  <name>Create v2 tables for all new features</name>
  <files>src/tracker/data/DatabaseSchema.java</files>
  <action>
    Add a new private static method createV2Tables(Connection conn) that creates these tables using CREATE TABLE IF NOT EXISTS:

    1. learning_profiles (id INTEGER PK AUTOINCREMENT, profile_name TEXT NOT NULL UNIQUE, description TEXT)
    2. study_strategies (id INTEGER PK AUTOINCREMENT, profile_id INTEGER NOT NULL FK→learning_profiles, strategy_name TEXT NOT NULL, instructions TEXT, UNIQUE(profile_id, strategy_name))
    3. questionnaire_questions (id INTEGER PK AUTOINCREMENT, question_text TEXT NOT NULL, option_a TEXT, option_b TEXT, option_c TEXT, option_d TEXT)
    4. student_questionnaire_responses (id INTEGER PK AUTOINCREMENT, student_id INTEGER NOT NULL FK→students ON DELETE CASCADE, question_id INTEGER NOT NULL FK→questionnaire_questions, answer_value TEXT NOT NULL, responded_at TEXT DEFAULT datetime('now'))
    5. student_learning_profiles (id INTEGER PK AUTOINCREMENT, student_id INTEGER NOT NULL FK→students ON DELETE CASCADE, profile_id INTEGER NOT NULL FK→learning_profiles, assigned_date TEXT DEFAULT datetime('now'), UNIQUE(student_id))
    6. topics (id INTEGER PK AUTOINCREMENT, subject_id INTEGER NOT NULL FK→subjects ON DELETE CASCADE, topic_name TEXT NOT NULL, UNIQUE(subject_id, topic_name))
    7. topic_scores (id INTEGER PK AUTOINCREMENT, score_id INTEGER NOT NULL FK→student_scores ON DELETE CASCADE, topic_id INTEGER NOT NULL FK→topics ON DELETE CASCADE, score_value REAL NOT NULL, proficiency_level TEXT DEFAULT 'UNKNOWN')
    8. student_goals (id INTEGER PK AUTOINCREMENT, student_id INTEGER NOT NULL FK→students ON DELETE CASCADE, subject_id INTEGER FK→subjects, target_score REAL NOT NULL, deadline TEXT, status TEXT NOT NULL DEFAULT 'ACTIVE', created_at TEXT DEFAULT datetime('now'))
    9. intervention_plans (id INTEGER PK AUTOINCREMENT, student_id INTEGER NOT NULL FK→students ON DELETE CASCADE, teacher_id INTEGER NOT NULL FK→users, intervention_type TEXT NOT NULL, description TEXT, start_date TEXT DEFAULT datetime('now'), status TEXT NOT NULL DEFAULT 'ACTIVE')
    10. intervention_logs (id INTEGER PK AUTOINCREMENT, plan_id INTEGER NOT NULL FK→intervention_plans ON DELETE CASCADE, notes TEXT, outcome_metric TEXT, log_date TEXT DEFAULT datetime('now'))
    11. exam_artifacts (id INTEGER PK AUTOINCREMENT, score_id INTEGER FK→student_scores ON DELETE SET NULL, teacher_id INTEGER FK→users, file_path TEXT NOT NULL, original_filename TEXT, upload_date TEXT DEFAULT datetime('now'), feedback_text TEXT)
    12. reevaluation_requests (id INTEGER PK AUTOINCREMENT, score_id INTEGER NOT NULL FK→student_scores, student_id INTEGER NOT NULL FK→students ON DELETE CASCADE, reason TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'PENDING', submitted_at TEXT DEFAULT datetime('now'), resolved_by INTEGER FK→users, resolution_notes TEXT, resolved_at TEXT)

    Also add indexes on frequently queried columns:
    - idx_topic_scores_score ON topic_scores(score_id)
    - idx_intervention_plans_student ON intervention_plans(student_id)
    - idx_reevaluation_requests_student ON reevaluation_requests(student_id)
    - idx_student_goals_student ON student_goals(student_id)

    Also add a new seedV2Defaults(conn) method that inserts:
    - 5 learning profiles: 'Memorization-Heavy', 'Conceptual', 'Practice-Based', 'Visual', 'Mixed'
    - 3 questionnaire questions about learning preferences
  </action>
  <verify>
    Compile the full project:
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/Main.java
    Delete alip_data.db and re-run to test fresh creation:
    java -cp "bin;lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar;lib/slf4j-nop-2.0.9.jar" tracker.Main
    Then verify with: sqlite3 alip_data.db ".tables" should show all 18 tables
    And: sqlite3 alip_data.db "SELECT COUNT(*) FROM learning_profiles;" should return 5
  </verify>
  <done>All 12 new tables are created. Seed data for learning_profiles is inserted. Schema version is 2. Existing tables and data are preserved on migration.</done>
</task>

## Success Criteria
- [ ] `PRAGMA user_version` returns 2 after app initialization
- [ ] All 12 new tables exist in the database
- [ ] Learning profiles seed data is present (5 rows)
- [ ] Existing tables (roles, users, students, subjects, student_scores, configuration) are untouched
- [ ] Project compiles without errors
