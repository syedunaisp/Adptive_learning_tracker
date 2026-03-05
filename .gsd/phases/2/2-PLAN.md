---
phase: 2
plan: 2
wave: 1
---

# Plan 2.2: GoalTracker + ArtifactManager + ReevaluationWorkflow

## Objective
Create three more service classes: `GoalTracker` for student goal lifecycle, `ArtifactManager` for file upload storage, and `ReevaluationWorkflow` for the re-evaluation request pipeline.

## Context
- src/tracker/data/dao/GoalDAO.java
- src/tracker/data/dao/ArtifactDAO.java
- src/tracker/data/dao/ReevaluationDAO.java
- src/tracker/model/Goal.java
- src/tracker/model/ExamArtifact.java
- src/tracker/model/ReevaluationRequest.java

## Tasks

<task type="auto">
  <name>Create GoalTracker service</name>
  <files>src/tracker/service/GoalTracker.java [NEW]</files>
  <action>
    Service class wrapping GoalDAO:

    GoalTracker:
    - Constructor takes GoalDAO
    - setGoal(int studentDbId, Integer subjectDbId, double targetScore, String deadline) → boolean
    - getActiveGoals(int studentDbId) → List of Goal (filter by status=ACTIVE)
    - checkGoalProgress(int studentDbId, double currentAverage) → List of String
      Logic: For each ACTIVE goal, compare currentAverage or subject score against targetScore.
      Return progress messages like "Math goal: 72.0/80.0 (90% progress)" or "ACHIEVED: Overall avg goal reached!"
      Auto-mark goals as ACHIEVED if target is met.
    - abandonGoal(int goalId) → boolean (set status to ABANDONED)
    - getAllGoals(int studentDbId) → List of Goal (all statuses)
  </action>
  <verify>
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/service/GoalTracker.java
  </verify>
  <done>GoalTracker compiles with auto-achievement detection and progress reporting.</done>
</task>

<task type="auto">
  <name>Create ArtifactManager and ReevaluationWorkflow services</name>
  <files>
    src/tracker/service/ArtifactManager.java [NEW]
    src/tracker/service/ReevaluationWorkflow.java [NEW]
  </files>
  <action>
    ArtifactManager:
    - Constructor takes ArtifactDAO
    - uploadArtifact(int scoreId, int teacherDbId, String sourcePath, String originalFilename, String feedback) → boolean
      Logic: Copy file to "./uploads/" directory with a timestamped filename. Store the destination path via DAO. Create the uploads dir if it doesn't exist.
    - getArtifactsForScore(int scoreId) → List of ExamArtifact
    - getArtifactsForStudent(int studentDbId) → List of ExamArtifact
    - deleteArtifact(int artifactId) → boolean
      Logic: Retrieve artifact record, delete the physical file if it exists, then delete the DB record.

    ReevaluationWorkflow:
    - Constructor takes ReevaluationDAO
    - submitRequest(int scoreId, int studentDbId, String reason) → boolean
      Validation: reason must not be empty, minimum 10 characters
    - getStudentRequests(int studentDbId) → List of ReevaluationRequest
    - getPendingRequests() → List of ReevaluationRequest (for teacher view)
    - approveRequest(int requestId, int teacherUserId, String notes) → boolean
    - rejectRequest(int requestId, int teacherUserId, String notes) → boolean

    No Swing/AWT imports.
  </action>
  <verify>
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/service/ArtifactManager.java src/tracker/service/ReevaluationWorkflow.java
  </verify>
  <done>Both services compile. ArtifactManager handles file copy + DB record. ReevaluationWorkflow handles the approve/reject pipeline.</done>
</task>

## Success Criteria
- [ ] GoalTracker.java compiles with progress checking and auto-achievement
- [ ] ArtifactManager.java compiles with file I/O + DB integration
- [ ] ReevaluationWorkflow.java compiles with request validation
- [ ] No UI imports in any class
