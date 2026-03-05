---
updated: 2026-03-06
---

# Project State

## Current Position

**Milestone:** ALIP v4.0
**Phase:** 1 - Foundation & Backend Extensions
**Status:** planned — ready for execution
**Plan:** 3 plans created (1.1, 1.2, 1.3)

## Last Action

Created Phase 1 execution plans:
- Plan 1.1 (Wave 1): Schema versioning + 12 new table DDLs + seed data
- Plan 1.2 (Wave 1): 6 new DAOs (GoalDAO, InterventionDAO, ProfileDAO, AnalyticsDAO, ArtifactDAO, ReevaluationDAO)
- Plan 1.3 (Wave 2): RiskScore percentage methods + 9 new model POJOs

## Next Steps

1. Run `/execute 1` to implement Phase 1
2. Wave 1 first (Plans 1.1 + 1.2 in parallel)
3. Wave 2 next (Plan 1.3 depends on 1.1 and 1.2)

## Active Decisions

| Decision | Choice | Made | Affects |
|----------|--------|------|---------|
| UI Framework | JavaFX (migrate from Swing) | 2026-03-06 | Phase 3-5 |
| UI Pattern | MVVM with FXML | 2026-03-06 | Phase 3-5 |
| No Swing/JavaFX coexistence | Build JavaFX in parallel, cutover at end | 2026-03-06 | Phase 3-5 |
| Backend untouched | DAO/Service/AI layers must not break | 2026-03-06 | All phases |
| Schema versioning | Use PRAGMA user_version | 2026-03-06 | Phase 1 |
| File storage for uploads | Local filesystem, not BLOB | 2026-03-06 | Phase 1, 4 |
| DAO return types | Use typed model POJOs, not Map<String,Object> | 2026-03-06 | Phase 1 |
| RiskScore already has components | Only add percentage contribution methods, not a new DTO | 2026-03-06 | Phase 1 |

## Blockers

None

## Concerns

- Build system is manual `javac` — JavaFX modules may need `--module-path` flags
- JavaFX is not bundled with JDK 21+ — need to ensure OpenJFX SDK is available

## Session Context

Phase 1 is fully planned. 3 plans across 2 waves. Ready for `/execute 1`.
