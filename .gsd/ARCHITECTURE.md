# ALIP Architecture

> Current system architecture for the Adaptive Learning Intelligence Platform.

## Layer Diagram

```
┌──────────────────────────────────────────┐
│               UI Layer                    │
│  (Swing → JavaFX migration planned)      │
│  LoginFrame, MainFrame, DashboardCard    │
│  StyleConstants, StyledButton            │
├──────────────────────────────────────────┤
│            Security Layer                 │
│  PasswordHasher, SessionManager          │
├──────────────────────────────────────────┤
│            Service Layer                  │
│  AnalyticsService, SimulationService     │
│  ┌─ AI ─────────────────────────────┐    │
│  │ RiskPredictor, TrendAnalyzer,    │    │
│  │ AdaptivePlanner                  │    │
│  └──────────────────────────────────┘    │
├──────────────────────────────────────────┤
│           Data Access Layer               │
│  DataManager, DBConnectionManager        │
│  DatabaseSchema, DataMigration           │
│  ┌─ DAO ────────────────────────────┐    │
│  │ UserDAO, StudentDAO, ScoreDAO,   │    │
│  │ ConfigDAO, SubjectDAO            │    │
│  └──────────────────────────────────┘    │
├──────────────────────────────────────────┤
│           Model Layer                     │
│  Student, Subject, User, UserRole        │
│  RiskScore, SimulationResult             │
│  TrendDirection, SubjectCategory         │
├──────────────────────────────────────────┤
│           SQLite Database                 │
│  alip_data.db (WAL mode, FK enforced)    │
└──────────────────────────────────────────┘
```

## Package Structure

```
src/tracker/
├── Main.java
├── model/         # Domain entities (8 classes)
├── data/          # Persistence orchestration
│   └── dao/       # SQL execution (5 DAOs)
├── security/      # Auth & session (2 classes)
├── service/       # Business logic
│   └── ai/        # AI analytics (3 classes)
└── ui/            # Presentation (5 classes)
```

## Key Patterns

- **Layered Architecture**: Strict separation between UI → Service → DAO → DB
- **DAO Pattern**: All SQL isolated in `data.dao/` package
- **Role-Based Access**: `UserRole` enum with `SessionManager` gating
- **Configurable AI**: Risk weights stored in DB, loaded via `ConfigDAO`
