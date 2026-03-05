# ALIP - Adaptive Learning Intelligence Platform (v3.0)

> **Database-Driven, Multi-User AI-powered Academic Risk Intelligence Platform**

## Overview

ALIP v3.0 is a complete upgrade of the original Java Swing application, transforming it from a single-user, in-memory tool into a **multi-user, role-aware, database-driven platform**. It preserves the powerful **weighted multi-factor risk scoring engine**, **trend analysis**, and **adaptive recommendation system** while adding enterprise-grade security and persistence.

Built with clean layered architecture principles — using **SQLite** for zero-config embedded data storage and **pure Java 21+**.

---

## 🚀 New Features in v3.0

### 1. Database Persistence (SQLite)
- All data is now securely stored in an embedded `alip_data.db` SQLite database.
- Complete DAO (Data Access Object) layer separates SQL from business logic.
- Referential integrity (Foreign Keys) and cascading deletes are fully enforced.

### 2. Role-Based Access Control & Security
- **Authentication:** Login system with SHA-256 + per-password salt hashing.
- **Three distinct roles:**
  - **ADMIN:** Full system access (User management, Risk configuration).
  - **TEACHER:** Manage students (Add/Edit/Delete), access Analytics & Simulations.
  - **STUDENT:** View-only access to their own personal profile and recommendations.
- **Session Management:** Secure runtime tracking of logged-in user permissions.

### 3. Role-Aware Dashboards
- **Teacher/Admin Dashboard:** Institutional overview with system-wide metric cards (Total Students, High Risk Count, Average, Weakest Subject).
- **Student Profile Dashboard:** A personalized view showing only the logged-in student's average, risk level, trend, subject scores, and targeted AI recommendations.

### 4. Admin & Teacher Management Panels
- **User Management (Admin):** Create users, assign roles, link student IDs to student accounts, and toggle account access.
- **Risk Configuration (Admin):** Dynamically adjust risk engine weights and thresholds directly from the database.
- **Student CRUD (Teacher):** A dedicated "Manage Students" page to edit student names, modify specific subject scores, or permanently delete student records.

### 5. UI/UX Modernization
- Complete visual overhaul of the Login and Main screens.
- SaaS-style sidebar navigation with Unicode icons.
- Modern shadow-casting cards, rounded input fields, and hover-darken buttons.
- Clean, grid-based "At-a-Glance" metric cards.
- Custom table renderers (color-coded Risk Badges and Trend Arrows).

### 6. Legacy Data Migration
- Built-in tool to import legacy `academic_report.txt` files directly into the new relational database structure.

---

## 🔑 Default Credentials

The database is automatically created and seeded on first run. Use these credentials to log in:

| Role    | Username | Password     | Access Level |
|---------|----------|--------------|--------------|
| **Admin**   | `admin`    | `admin123`     | Full system & user management |
| **Teacher** | `teacher`  | `teacher123`   | Student CRUD, Analytics, Simulation |

*(Student accounts can be created by the Admin and linked to a specific Student ID).*

---

## 🛠️ Build and Run Instructions

This project uses standard `javac` and requires the provided libraries in the `lib/` folder (SQLite JDBC + SLF4J).

### Windows (PowerShell/CMD)

```bash
# 1. Navigate to the project directory
cd C:\Users\syedu\OneDrive\Documents\codes\IRP

# 2. Clean previous builds
rm -rf bin/tracker

# 3. Compile the project (include lib jars in classpath)
javac -d bin -cp "lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar" -sourcepath src src/tracker/Main.java

# 4. Run the application
java -cp "bin;lib/sqlite-jdbc-3.45.1.0.jar;lib/slf4j-api-2.0.9.jar;lib/slf4j-nop-2.0.9.jar" tracker.Main
```

*Note: If you are using Java 21+, you may see a warning about `System::load` dynamically loading an agent (from the SQLite driver). This is harmless and can be ignored, or suppressed by adding `--enable-native-access=ALL-UNNAMED` to the `java` command.*

---

## 🏛️ Architecture

```text
src/tracker/
|-- Main.java                         # Bootstraps DB and launches UI
|
|-- model/                            # Domain entities
|   |-- User.java / UserRole.java     # Authentication & Authorization
|   +-- Student.java / Subject.java   # Academic data models
|
|-- data/                             # Data Access Layer
|   |-- DBConnectionManager.java      # SQLite connection pooling/setup
|   |-- DatabaseSchema.java           # DDL & Seed data execution
|   |-- DataManager.java              # Cache & DAO orchestration
|   |-- DataMigration.java            # Legacy report importer
|   +-- dao/                          # SQL execution classes
|       |-- UserDAO.java
|       |-- StudentDAO.java
|       |-- ScoreDAO.java
|       +-- ConfigDAO.java
|
|-- security/                         # Security Layer
|   |-- PasswordHasher.java           # SHA-256 + Salt cryptography
|   +-- SessionManager.java           # Runtime context
|
|-- service/                          # Business / AI Logic (Untouched!)
|   |-- AnalyticsService.java
|   |-- SimulationService.java
|   +-- ai/
|       |-- RiskPredictor.java        # DB-configurable risk engine
|       |-- TrendAnalyzer.java
|       +-- AdaptivePlanner.java
|
+-- ui/                               # Presentation Layer
    |-- LoginFrame.java               # Secure DB-backed login screen
    |-- MainFrame.java                # Role-aware SaaS dashboard
    +-- StyleConstants.java           # Modern palette & typography
```

---

## 🧠 AI-Powered Risk Scoring (Preserved Core)

The core AI risk engine remains fully intact, but its parameters are now configurable via the database by Admins.

**Risk Score Formula:**
`Risk = (W1 * AvgRisk) + (W2 * WeakDensity) + (W3 * LowestSeverity) + (W4 * TrendPenalty)`

| Level    | Score Range | Description                              |
|----------|-------------|------------------------------------------|
| **High Risk**   | >= 60.0     | Immediate intervention recommended       |
| **Moderate Risk** | 35.0 - 59.9 | Proactive measures needed              |
| **Low Risk**    | < 35.0      | Student performing well                  |

---

## 📚 Technical Highlights

- **Embedded Database:** SQLite 3.45 with Write-Ahead Logging (WAL) and enforced Foreign Keys.
- **Clean Architecture:** Strict separation between Presentation (Swing), Business Logic (Services), and Persistence (DAOs).
- **Security Best Practices:** Passwords are never stored or compared in plain text. Uses unique randomized salts per user.
- **Graceful Degradation:** If the database cannot be initialized, the app halts safely with a user-friendly error dialog rather than crashing silently.