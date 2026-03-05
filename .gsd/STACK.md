# ALIP Technology Stack

## Runtime
- **Language:** Java 21+
- **UI (current):** Java Swing
- **UI (target):** JavaFX (OpenJFX)
- **Database:** SQLite 3.45 (embedded)

## Libraries
- `sqlite-jdbc-3.45.1.0.jar` — JDBC driver for SQLite
- `slf4j-api-2.0.9.jar` — Logging facade
- `slf4j-nop-2.0.9.jar` — No-op logging binding

## Build
- Manual `javac` compilation (no Maven/Gradle)
- Output to `bin/` directory
- Classpath includes `lib/*.jar`

## Security
- SHA-256 + per-password salt hashing
- Role-based access (ADMIN / TEACHER / STUDENT)

## AI/Analytics
- Weighted multi-factor risk scoring (configurable via DB)
- Trend analysis (time-series direction)
- Adaptive study recommendations (rule-based)
