---
phase: 1
plan: 2
wave: 1
---

# Plan 1.2: New DAO Classes

## Objective
Create the new Data Access Objects required to support the 12 new features. These DAOs follow the exact same pattern as the existing `ScoreDAO`, `StudentDAO`, etc. — using `DBConnectionManager.getConnection()`, `PreparedStatement`, and `closeQuietly()` for resource management.

## Context
- .gsd/SPEC.md
- .gsd/ARCHITECTURE.md
- src/tracker/data/dao/ScoreDAO.java (reference pattern)
- src/tracker/data/DBConnectionManager.java

## Tasks

<task type="auto">
  <name>Create GoalDAO, InterventionDAO, and ProfileDAO</name>
  <files>
    src/tracker/data/dao/GoalDAO.java [NEW]
    src/tracker/data/dao/InterventionDAO.java [NEW]
    src/tracker/data/dao/ProfileDAO.java [NEW]
  </files>
  <action>
    Create three new DAO classes following the existing ScoreDAO pattern exactly:

    GoalDAO.java:
    - insertGoal(int studentDbId, Integer subjectDbId, double targetScore, String deadline) → boolean
    - getGoalsByStudent(int studentDbId) → List of goal records (return as List of Map or create a Goal model)
    - updateGoalStatus(int goalId, String status) → boolean
    - deleteGoal(int goalId) → boolean

    InterventionDAO.java:
    - insertPlan(int studentDbId, int teacherDbId, String type, String description) → int (returns plan ID)
    - getPlansByStudent(int studentDbId) → List of intervention plan records
    - getPlansByTeacher(int teacherDbId) → List of intervention plan records
    - updatePlanStatus(int planId, String status) → boolean
    - insertLog(int planId, String notes, String outcomeMetric) → boolean
    - getLogsByPlan(int planId) → List of intervention log records

    ProfileDAO.java:
    - getAllProfiles() → List of learning profile records
    - getStrategiesByProfile(int profileId) → List of strategy records
    - saveStudentProfile(int studentDbId, int profileId) → boolean
    - getStudentProfile(int studentDbId) → profile record or null
    - saveQuestionnaireResponse(int studentDbId, int questionId, String answer) → boolean
    - getQuestionnaireQuestions() → List of question records

    All methods must: use try/catch/finally, call closeQuietly(), print errors to stderr.
    Return simple data structures (Map<String, Object> or new model classes).
    DO NOT import any Swing or UI classes.
  </action>
  <verify>
    Compile: javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/data/dao/GoalDAO.java src/tracker/data/dao/InterventionDAO.java src/tracker/data/dao/ProfileDAO.java
    All three files must compile without errors.
  </verify>
  <done>Three new DAO classes created and compiling. Each follows the established ScoreDAO JDBC pattern. No UI imports.</done>
</task>

<task type="auto">
  <name>Create AnalyticsDAO, ArtifactDAO, and ReevaluationDAO</name>
  <files>
    src/tracker/data/dao/AnalyticsDAO.java [NEW]
    src/tracker/data/dao/ArtifactDAO.java [NEW]
    src/tracker/data/dao/ReevaluationDAO.java [NEW]
  </files>
  <action>
    Create three more DAO classes:

    AnalyticsDAO.java:
    - getClassAverage() → double
    - getTop10PercentAverage() → double
    - getStudentPercentile(int studentDbId) → double (0.0-100.0)
    - getSubjectAverages() → Map of subjectName→averageScore (for institutional dashboard)
    - getRiskDistribution() → Map of riskLevel→count (requires calling RiskPredictor or querying precomputed data)
    - getTopicScoresByStudent(int studentDbId, int subjectDbId) → List of topic score records

    ArtifactDAO.java:
    - insertArtifact(int scoreId, int teacherDbId, String filePath, String originalFilename, String feedback) → boolean
    - getArtifactsByScore(int scoreId) → List of artifact records
    - getArtifactsByStudent(int studentDbId) → List of artifact records (JOIN with student_scores)
    - deleteArtifact(int artifactId) → boolean

    ReevaluationDAO.java:
    - submitRequest(int scoreId, int studentDbId, String reason) → boolean
    - getRequestsByStudent(int studentDbId) → List of request records
    - getPendingRequests() → List of request records (for teacher view)
    - resolveRequest(int requestId, int resolvedByUserId, String notes) → boolean (sets status='RESOLVED', resolved_at=now)
    - rejectRequest(int requestId, int resolvedByUserId, String notes) → boolean (sets status='REJECTED')

    Follow the exact same coding pattern as ScoreDAO. Use DBConnectionManager for connections.
  </action>
  <verify>
    Compile all new DAOs together with the project:
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/Main.java
    Zero compilation errors across the full project.
  </verify>
  <done>Six new DAO classes total are created and the full project compiles. All follow the existing JDBC pattern.</done>
</task>

## Success Criteria
- [ ] 6 new DAO files exist in src/tracker/data/dao/
- [ ] Full project compiles with zero errors
- [ ] No DAO imports any Swing/AWT class
- [ ] All DAOs use DBConnectionManager and closeQuietly pattern
