# SPEC.md — ALIP v4.0 Specification

> **Status**: `FINALIZED`
>
> ⚠️ **Planning Lock**: No code may be written until this spec is marked `FINALIZED`.

## Vision

ALIP (Adaptive Learning Intelligence Platform) v4.0 extends the existing v3.0 Java desktop application with 12 advanced analytics and intervention features, while simultaneously migrating the UI layer from Java Swing to JavaFX. The backend (model, data, service, security layers) remains untouched. The result is a modern, CSS-styled, MVVM-architected platform with deep learning analytics, intervention planning, and institutional intelligence.

## Goals

1. **Advanced Analytics** — Add peer benchmarking, learning gap mapping, trajectory prediction, and institutional dashboards.
2. **Intervention System** — Add personalized study strategy advising, intervention planning/logging, and intervention simulation.
3. **Student Empowerment** — Add goal tracking, answer sheet viewing, and re-evaluation requests.
4. **Explainable AI** — Add risk explanation panel showing factor contributions.
5. **JavaFX Migration** — Replace the entire Swing UI layer with a JavaFX MVVM architecture using FXML and CSS theming. Backend layers must NOT be rewritten.

## Non-Goals (Out of Scope)

- Rewriting the DAO, Service, or AI layers
- Moving to a web or mobile platform
- Adding real-time collaboration or chat features
- Integrating with external LMS (Moodle, Canvas, etc.)
- Using external cloud databases (SQLite remains)
- Swing/JavaFX coexistence via JFXPanel (too risky)

## Constraints

- **Language:** Java 21+, no additional language runtimes
- **Database:** Embedded SQLite (existing `alip_data.db`)
- **UI Framework:** JavaFX (migration from Swing)
- **Backend Stability:** `model/`, `data/`, `data.dao/`, `service/`, `service.ai/`, `security/` packages must not break
- **Build:** Manual `javac` compilation (no Maven/Gradle currently)
- **Timeline:** Active development, incremental delivery required

## Success Criteria

- [ ] All 12 new features are implemented and functional
- [ ] UI is fully migrated from Swing to JavaFX
- [ ] All existing functionality (login, CRUD, risk scoring, analytics, simulation) works in the JavaFX UI
- [ ] Backend service and DAO layers have zero Swing imports
- [ ] CSS theming replaces hardcoded `StyleConstants.java`
- [ ] Role-based access control works identically in the new UI
- [ ] Database schema versioning prevents data loss on upgrades

## User Stories

### As a Student
- I want to see why I was flagged as high-risk, so that I understand what to improve
- I want personalized study strategies based on my learning style
- I want to set academic goals and track my progress
- I want to compare my performance to class averages
- I want to see my topic-level strengths and weaknesses
- I want to view my graded answer sheets and teacher feedback
- I want to request re-evaluation of a grade I believe is incorrect

### As a Teacher
- I want to plan interventions for high-risk students
- I want to simulate score changes and see their effect on risk levels
- I want to log interventions and track their outcomes
- I want to see predicted student trajectories
- I want to upload answer sheets with feedback

### As an Admin
- I want to see institution-wide analytics (hardest subjects, risk distribution, intervention success)
- I want all existing admin capabilities to work in the new UI

## Technical Requirements

| Requirement | Priority | Notes |
|---|---|---|
| Risk Explanation DTO (`RiskAnalysisResult`) | Must-have | Refactor `RiskPredictor` return type |
| Simple Linear Regression in `TrendAnalyzer` | Must-have | Powers trajectory prediction |
| Database schema versioning (`PRAGMA user_version`) | Must-have | Safe migration of existing DBs |
| New DAO classes (Analytics, Intervention, Goal, Artifact) | Must-have | Support new features |
| MVVM ViewModel layer for JavaFX | Must-have | Clean UI separation |
| FXML-based views with CSS theming | Must-have | Modern, maintainable UI |
| `ViewManager` routing (replaces `CardLayout`) | Must-have | JavaFX navigation |
| File storage for answer sheet uploads | Should-have | Local filesystem, not BLOB |
| `InterventionEngine` rule-based logic | Should-have | Smart intervention suggestions |
| `ProfileService` questionnaire analysis | Should-have | Learning style detection |

---

*Last updated: 2026-03-06*
