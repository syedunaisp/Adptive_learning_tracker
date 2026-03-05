---
milestone: ALIP v4.0
version: 4.0.0
updated: 2026-03-06
---

# Roadmap

> **Current Phase:** 1 - Foundation & Backend Extensions
> **Status:** planning

## Must-Haves (from SPEC)

- [ ] 12 new features implemented
- [ ] Swing → JavaFX UI migration complete
- [ ] Backend layers untouched (no swing imports)
- [ ] Database schema versioning
- [ ] CSS theming replaces StyleConstants

---

## Phases

### Phase 1: Foundation & Backend Extensions
**Status:** 🔄 In Progress (planned, ready for execution)
**Objective:** Extend the database schema, add new DAOs, create DTOs and refactor RiskPredictor to return RiskAnalysisResult. Add schema versioning. This phase touches NO UI code.
**Requirements:** Schema versioning, new tables, DTO refactor, new DAOs

**Plans:**
- [ ] Plan 1.1: Schema versioning + new table DDLs
- [ ] Plan 1.2: New DAOs (AnalyticsDAO, InterventionDAO, GoalDAO, ArtifactDAO, ProfileDAO)
- [ ] Plan 1.3: RiskPredictor DTO refactor (RiskAnalysisResult)

---

### Phase 2: Service Layer Extensions
**Status:** ⬜ Not Started
**Objective:** Add new business logic services (ProfileService, InterventionEngine, WorkflowService, ArtifactManager). Extend TrendAnalyzer with trajectory prediction. All backend, no UI.
**Depends on:** Phase 1

**Plans:**
- [ ] Plan 2.1: ProfileService + InterventionEngine
- [ ] Plan 2.2: TrendAnalyzer regression + ArtifactManager + WorkflowService

---

### Phase 3: JavaFX Bootstrap & Login
**Status:** ⬜ Not Started
**Objective:** Create the JavaFX application entry point, global CSS themes, ViewManager router, and the Login screen. This is the first UI migration step.
**Depends on:** Phase 2

**Plans:**
- [ ] Plan 3.1: JavaFX bootstrapper + CSS theme + ViewManager
- [ ] Plan 3.2: LoginView (FXML + Controller + ViewModel)

---

### Phase 4: Dashboard & Feature Views Migration
**Status:** ⬜ Not Started
**Objective:** Rebuild all dashboard views, data tables, forms, and new feature panels in JavaFX with MVVM pattern.
**Depends on:** Phase 3

**Plans:**
- [ ] Plan 4.1: Student Dashboard + Risk Explanation + Goal Tracker + Gap Map
- [ ] Plan 4.2: Teacher Dashboard + Intervention Manager + Simulation Dialog
- [ ] Plan 4.3: Admin Dashboard + Institutional Analytics + Data Tables
- [ ] Plan 4.4: Answer Sheet Upload + Re-evaluation Requests + Peer Benchmarking

---

### Phase 5: Integration, Polish & Cutover
**Status:** ⬜ Not Started
**Objective:** Final integration testing, role-based access verification, remove Swing entry point, polish UI.
**Depends on:** Phase 4

**Plans:**
- [ ] Plan 5.1: Integration testing + role-based access verification
- [ ] Plan 5.2: Cutover (swap entry point) + cleanup

---

## Progress Summary

| Phase | Status | Plans | Complete |
|-------|--------|-------|----------|
| 1 | ⬜ | 0/3 | — |
| 2 | ⬜ | 0/2 | — |
| 3 | ⬜ | 0/2 | — |
| 4 | ⬜ | 0/4 | — |
| 5 | ⬜ | 0/2 | — |

---

## Timeline

| Phase | Started | Completed | Duration |
|-------|---------|-----------|----------|
| 1 | — | — | — |
| 2 | — | — | — |
| 3 | — | — | — |
| 4 | — | — | — |
| 5 | — | — | — |
