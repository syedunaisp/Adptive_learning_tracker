---
phase: 2
plan: 1
wave: 1
---

# Plan 2.1: ProfileService + InterventionEngine

## Objective
Create two new service classes that orchestrate the new DAOs. `ProfileService` manages the learning profile questionnaire workflow and strategy lookup. `InterventionEngine` manages the intervention lifecycle (create, log, status transitions).

## Context
- src/tracker/data/dao/ProfileDAO.java
- src/tracker/data/dao/InterventionDAO.java
- src/tracker/data/dao/GoalDAO.java
- src/tracker/model/LearningProfile.java
- src/tracker/model/InterventionPlan.java

## Tasks

<task type="auto">
  <name>Create ProfileService</name>
  <files>src/tracker/service/ProfileService.java [NEW]</files>
  <action>
    Create a new service class that wraps ProfileDAO with business logic:

    ProfileService:
    - Constructor takes ProfileDAO (dependency injection)
    - determineProfile(int studentDbId, Map of questionId→answer) → LearningProfile
      Logic: Count answer frequencies: mostly A → Memorization-Heavy (id=1), mostly B → Conceptual (id=2), mostly C → Practice-Based (id=3), mostly D → Visual (id=4), tie → Mixed (id=5).
      Saves responses AND the computed profile via DAO.
    - getStudentProfile(int studentDbId) → LearningProfile (delegates to DAO)
    - getStudentStrategies(int studentDbId) → List of StudyStrategy
      Logic: Get student profile, then get strategies for that profile
    - getAllProfiles() → List of LearningProfile (delegates)
    - getQuestionnaireQuestions() → List of String[] (delegates)

    No Swing/AWT imports. Pure business logic.
  </action>
  <verify>
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/service/ProfileService.java
  </verify>
  <done>ProfileService compiles and encapsulates the questionnaire→profile assignment flow.</done>
</task>

<task type="auto">
  <name>Create InterventionEngine</name>
  <files>src/tracker/service/InterventionEngine.java [NEW]</files>
  <action>
    Create a service class that wraps InterventionDAO with business logic:

    InterventionEngine:
    - Constructor takes InterventionDAO, GoalDAO (dependency injection)
    - createPlan(int studentDbId, int teacherDbId, String type, String description) → InterventionPlan
      Logic: Call DAO insertPlan, return the created plan with its generated ID
    - completePlan(int planId) → boolean (set status to COMPLETED)
    - cancelPlan(int planId) → boolean (set status to CANCELLED)
    - logActivity(int planId, String notes, String outcomeMetric) → boolean
    - getStudentPlans(int studentDbId) → List of InterventionPlan
    - getTeacherPlans(int teacherDbId) → List of InterventionPlan
    - getPlanLogs(int planId) → List of InterventionLog
    - suggestIntervention(RiskScore risk, LearningProfile profile) → String
      Logic: Rule-based suggestions based on risk level and learning profile type:
      HIGH risk + Memorization → "TUTORING: Intensive revision sessions with spaced repetition"
      HIGH risk + Practice-Based → "PRACTICE: Daily problem-solving drills with escalating difficulty"
      MODERATE risk → "TOPIC_REVIEW: Focus on weakest topics identified in gap analysis"
      LOW risk → "STRATEGY_ADJUSTMENT: Optimize current study habits for advanced performance"

    No Swing/AWT imports.
  </action>
  <verify>
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/service/InterventionEngine.java
  </verify>
  <done>InterventionEngine compiles with full lifecycle management and rule-based suggestion logic.</done>
</task>

## Success Criteria
- [ ] ProfileService.java compiles with questionnaire→profile logic
- [ ] InterventionEngine.java compiles with lifecycle + suggestion logic
- [ ] No UI imports in either class
