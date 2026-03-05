---
phase: 2
plan: 3
wave: 2
---

# Plan 2.3: TrendAnalyzer Regression + AnalyticsService Enhancement

## Objective
Enhance `TrendAnalyzer` with simple linear regression for academic trajectory prediction (Feature #5). Extend `AnalyticsService` with peer benchmarking (Feature #3) and learning gap mapping (Feature #4). This is Wave 2 because it builds on the DAOs and models from Phase 1.

## Context
- src/tracker/service/ai/TrendAnalyzer.java (existing — 112 lines)
- src/tracker/service/AnalyticsService.java (existing — 256 lines)
- src/tracker/data/dao/AnalyticsDAO.java
- src/tracker/model/TopicScore.java

## Tasks

<task type="auto">
  <name>Add simple linear regression to TrendAnalyzer</name>
  <files>src/tracker/service/ai/TrendAnalyzer.java</files>
  <action>
    ADD (not replace) the following capabilities to TrendAnalyzer:

    1. Change the previousAverages map to store a List of Double (history of all scores) instead of a single Double:
       - Map<String, List<Double>> scoreHistory (new field)
       - Keep the existing previousAverages map for backward compatibility

    2. Update recordAverage() to also append to scoreHistory list

    3. Add new method: predictFutureScore(String studentId, int periodsAhead) → double
       Implements simple linear regression (y = mx + b) on the score history:
       - x = index (0, 1, 2, ...)
       - y = historical scores
       - Calculate slope m = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
       - Calculate intercept b = (Σy - m*Σx) / n
       - Return predicted score at x = (n-1 + periodsAhead), clamped to 0-100
       - If fewer than 2 data points, return the last recorded average

    4. Add method: getScoreHistory(String studentId) → List of Double (read-only copy)

    5. Add method: getProjectedTrend(String studentId) → String
       Returns "Projected to reach X.X in next period" based on regression

    DO NOT modify existing method signatures or behavior.
    Keep thread-safety (synchronized).
  </action>
  <verify>
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/service/ai/TrendAnalyzer.java
  </verify>
  <done>TrendAnalyzer has linear regression prediction. Existing API unchanged. Thread-safe.</done>
</task>

<task type="auto">
  <name>Extend AnalyticsService with peer benchmarking and gap mapping</name>
  <files>src/tracker/service/AnalyticsService.java</files>
  <action>
    ADD the following methods to AnalyticsService:

    1. Peer Benchmarking (Feature #3):
    - getStudentPercentile(Student student, List<Student> allStudents) → double
      Calculate what percentile the student falls in relative to peers
    - getPeerComparison(Student student, List<Student> allStudents) → Map<String, Double>
      Returns: {"studentAvg": X, "classAvg": Y, "top10Avg": Z, "percentile": P}

    2. Learning Gap Mapping (Feature #4):
    - identifyLearningGaps(Student student) → List<String>
      Logic: For each subject, if score < 50 → "CRITICAL GAP: {subject}",
      if score < 70 → "GAP: {subject}", otherwise skip.
      Sort by severity (lowest scores first).

    3. Enhanced Institutional Intelligence (Feature #10):
    - getSubjectPerformanceRanking(List<Student> students) → Map<String, Double>
      Ranked list of subjects by average score (worst first)
    - getAtRiskCount(List<Student> students) → int
      Count of students at HIGH risk level

    DO NOT modify existing method signatures.
    Constructor already takes RiskPredictor and TrendAnalyzer — reuse them.
  </action>
  <verify>
    javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/Main.java
    Full project compilation — zero errors.
  </verify>
  <done>AnalyticsService has peer benchmarking, gap mapping, and enhanced institutional metrics. All existing methods untouched.</done>
</task>

## Success Criteria
- [ ] TrendAnalyzer has linear regression with predictFutureScore()
- [ ] AnalyticsService has peer benchmarking and gap mapping
- [ ] Existing method signatures unchanged
- [ ] Full project compiles
