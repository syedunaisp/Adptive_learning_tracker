---
phase: 1
plan: 3
wave: 2
---

# Plan 1.3: RiskPredictor Enhancement + New Model Classes

## Objective
Enhance the `RiskScore` model to support percentage-based factor contribution (for the Risk Explanation Panel UI). Add new model classes needed by the DAOs created in Plan 1.2. This plan depends on Plan 1.1 and 1.2 being complete.

## Context
- .gsd/SPEC.md
- src/tracker/model/RiskScore.java
- src/tracker/service/ai/RiskPredictor.java
- src/tracker/model/Student.java
- src/tracker/model/Subject.java

## Tasks

<task type="auto">
  <name>Add percentage contribution methods to RiskScore</name>
  <files>src/tracker/model/RiskScore.java</files>
  <action>
    The existing RiskScore already has component getters (getAverageComponent, getWeakCountComponent, etc.) and a getExplanations() list. It already IS the explanation DTO.

    Add four new convenience methods that return the PERCENTAGE CONTRIBUTION of each factor to the total score:
    - getAverageContributionPercent() → double (averageComponent / numericScore * 100, or 0 if numericScore is 0)
    - getWeakCountContributionPercent() → double
    - getLowestContributionPercent() → double
    - getTrendContributionPercent() → double

    These methods make it trivial for the UI to display a pie chart or bar chart of risk factor impact (e.g., "Average Score Impact: 40%").

    DO NOT change the constructor, the existing getters, or the scoring logic.
    DO NOT change the weights or thresholds.
    Only ADD new methods.
  </action>
  <verify>
    Compile:
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/model/RiskScore.java
    Must compile with zero errors. Existing tests or usages must not break.
  </verify>
  <done>RiskScore has four new percentage contribution methods. No existing behavior changed.</done>
</task>

<task type="auto">
  <name>Create new model classes for new features</name>
  <files>
    src/tracker/model/LearningProfile.java [NEW]
    src/tracker/model/StudyStrategy.java [NEW]
    src/tracker/model/Goal.java [NEW]
    src/tracker/model/InterventionPlan.java [NEW]
    src/tracker/model/InterventionLog.java [NEW]
    src/tracker/model/ExamArtifact.java [NEW]
    src/tracker/model/ReevaluationRequest.java [NEW]
    src/tracker/model/Topic.java [NEW]
    src/tracker/model/TopicScore.java [NEW]
  </files>
  <action>
    Create simple POJO model classes (no business logic, just fields + getters + setters + constructor):

    LearningProfile: id (int), profileName (String), description (String)
    StudyStrategy: id (int), profileId (int), strategyName (String), instructions (String)
    Goal: id (int), studentId (int), subjectId (Integer, nullable), targetScore (double), deadline (String), status (String), createdAt (String)
    InterventionPlan: id (int), studentId (int), teacherId (int), interventionType (String), description (String), startDate (String), status (String)
    InterventionLog: id (int), planId (int), notes (String), outcomeMetric (String), logDate (String)
    ExamArtifact: id (int), scoreId (int), teacherId (int), filePath (String), originalFilename (String), uploadDate (String), feedbackText (String)
    ReevaluationRequest: id (int), scoreId (int), studentId (int), reason (String), status (String), submittedAt (String), resolvedBy (Integer, nullable), resolutionNotes (String), resolvedAt (String)
    Topic: id (int), subjectId (int), topicName (String)
    TopicScore: id (int), scoreId (int), topicId (int), scoreValue (double), proficiencyLevel (String)

    Each class must:
    - Be in package tracker.model
    - Have a no-arg constructor and a full constructor
    - Have standard getters and setters
    - Override toString()
    - Have NO imports from javax.swing, java.awt, or any UI package

    Then update the DAOs from Plan 1.2 to use these model classes as return types instead of Map<String, Object>.
  </action>
  <verify>
    Compile entire project:
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/Main.java
    Zero errors. All new model classes compile. All DAOs compile with model type returns.
  </verify>
  <done>9 new model classes created. All DAOs updated to use typed model returns. Full project compiles.</done>
</task>

## Success Criteria
- [ ] RiskScore has 4 new percentage contribution methods
- [ ] 9 new model POJOs exist in src/tracker/model/
- [ ] DAOs from Plan 1.2 use typed model returns
- [ ] Full project compiles with zero errors
- [ ] No UI imports in any model or DAO class
