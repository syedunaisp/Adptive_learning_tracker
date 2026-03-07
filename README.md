# ALIP - Adaptive Learning Intelligence Platform (v4.0)

> **Database-Driven, Multi-User AI-powered Academic Risk Intelligence Platform**

## Overview

ALIP v4.0 is a complete upgrade of the original Java Swing application, transforming it from a single-user, in-memory tool into a **multi-user, role-aware, database-driven platform featuring a modern JavaFX interface**. It preserves the powerful **weighted multi-factor risk scoring engine**, **trend analysis**, and **adaptive recommendation system** while adding enterprise-grade security, comprehensive dashboard capabilities, and persistence.

Built with clean layered architecture principles — using **SQLite** for zero-config embedded data storage and **JavaFX 21+** for a dynamic, reactive user experience.

---

## 🚀 Key Features by Component

### 1. Advanced Academic Analytics
- **Peer Benchmarking:** Compare student performance against class averages and the top 10% percentiles.
- **Learning Gap Mapping:** Isolate topic-level strengths and weaknesses to prevent compounding knowledge deficits.
- **Trajectory Prediction:** Calculate moving averages via `TrendAnalyzer` to predict future grades.
- **Institutional Dashboards:** Aggregated system-wide metric monitoring for Admins.

### 2. Intelligent Intervention System
- **Study Strategy Advising:** Evaluate students using the `ProfileService` to determine learning styles (Visual, Abstract, Practice-based) and recommend tailored strategies.
- **Intervention Planning:** Teachers can create, assign, and track targeted coaching plans using the `InterventionEngine`.
- **"What-If" Simulations:** Safely simulate score changes (e.g. +10 on Math) using `SimulationService` to instantly see how it would affect Risk scores before committing actual grades.

### 3. Student Empowerment & Workflow
- **Goal Tracking:** Students can set personal academic goals (`GoalTracker`) and map them to their learning profiles.
- **Exam Artifacts & Feedback:** securely upload, store, and review graded answer sheets directly inside the platform.
- **Re-evaluation Workflow:** Request grade revisions securely through the `ReevaluationWorkflow`, linking teachers to automated approval/rejection loops.

### 4. Comprehensive Student Management 
- **Roster Building:** Organize students into designated `classes` and `sections`.
- **Bulk Excel Import:** Integrated **Apache POI** (v5.2.5) enables teachers to natively import rosters via `.xlsx` files without technical overhead.
- Augmented student entities mapping Roll Numbers, Emails, and Class IDs directly to the backend database.

### 5. Role-Aware JavaFX Dashboards
- **Student Dashboard:** View GPA metrics, recent trends, AI recommendations (`AdaptivePlanner`), and active re-evaluation requests.
- **Teacher Dashboard:** Detailed class metrics table with real-time risk/trend badges, What-If simulation panels, and full Student Management controls.
- **Admin Dashboard:** High-level system metrics, comprehensive user management table (create/disable users, link students), and read-only settings dialog detailing schema internals and risk configurations.

### 6. Database Persistence & Security (SQLite)
- **v4 Schema Structure:** `alip_data.db` manages 13+ robust tables handling users, roles, subject scores, topic sub-scores, interventions, and re-evaluations.
- **Authentication & RBAC:** Unique per-password salted hashing (SHA-256) combined with a SessionManager tracking strict Admin, Teacher, and Student permissions.

---

## 🔑 Default Credentials

The database is automatically created and seeded on first run. Use these credentials to log in:

| Role    | Username | Password     | Access Level |
|---------|----------|--------------|--------------|
| **Admin**   | `admin`    | `admin123`     | Full system & user management |
| **Teacher** | `teacher`  | `teacher123`   | Student Management, Analytics, Simulation |

*(Student accounts can be created by the Admin and linked to a specific Student ID).*

---

## 🛠️ Build and Run Instructions

This project requires standard `javac` and the provided libraries in the `lib/` folder (SQLite JDBC, SLF4J, OpenJFX 21.0.2, Apache POI).

### Windows (PowerShell/CMD)

The simplest way to run the application is to use the provided `run.ps1` script, which configures the classpath, attaches the OpenJFX modules, and passes necessary flags automatically:

```powershell
# 1. Navigate to the project directory
cd C:\Users\syedu\OneDrive\Documents\codes\IRP

# 2. Run the application
.\run.ps1
```

If you prefer compiling and running manually:

```powershell
# 1. Compile the project (include lib jars in classpath and JavaFX module paths)
$cp = (Get-ChildItem -Path lib/*.jar | Select-Object -ExpandProperty FullName) -join ';'
$jfx = "lib/javafx/javafx-sdk-21.0.2/lib"
javac -d bin -cp $cp --module-path $jfx --add-modules javafx.controls,javafx.fxml (Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName)

# 2. Copy the FXML and CSS assets to the bin directory (if not already copied)

# 3. Run the compiled application
$javacDir = (Get-Command javac).Source | Split-Path
& "$javacDir\java.exe" --enable-native-access=ALL-UNNAMED --module-path $jfx --add-modules javafx.controls,javafx.fxml -cp "bin;$cp" tracker.ui.fx.ALIPApplication
```

*Note: You may see a warning about dynamically loading an agent (from the SQLite driver). This is safely ignored by `--enable-native-access=ALL-UNNAMED` in the run script.*

---

## 🏛️ Architecture

```text
src/tracker/
|-- data/                             # Data Access Layer & Persistence
|   |-- DBConnectionManager.java      # SQLite connection pooling
|   |-- DatabaseSchema.java           # DDL, Migrations & Seeding
|   |-- DataManager.java              # Object cache orchestration
|   +-- dao/                          # SQL execution interfaces (UserDAO, ClassDAO, etc.)
|
|-- model/                            # Domain entities
|   +-- User, Student, ClassRoom, etc.
|
|-- security/                         # Cryptography & Access Control
|   |-- PasswordHasher.java
|   +-- SessionManager.java 
|
|-- service/                          # Business / AI Logic
|   |-- StudentManagementService.java # Roster & Apache POI Imports
|   |-- AnalyticsService.java         # Metric Aggregators
|   |-- ProfileService.java           # Learning Style Mapping
|   |-- InterventionEngine.java
|   |-- SimulationService.java
|   +-- ai/                           # Core algorithms (RiskPredictor, TrendAnalyzer)
|
+-- ui/fx/                            # Presentation Layer (JavaFX)
    |-- ALIPApplication.java          # Application Entry Point
    |-- ViewManager.java              # Role-aware generic scene router
    |-- controller/                   # View logic (TeacherDashboardController, Admin, etc.)
    +-- view/                         # FXML & CSS Layout files
```

---

## 📚 Technical Highlights

- **Embedded Database:** SQLite 3.45 with Write-Ahead Logging (WAL) and enforced Foreign Keys.
- **Clean Architecture:** Strict separation between Presentation (JavaFX), Business Logic (Services), and Persistence (DAOs).
- **Security Best Practices:** Passwords are never stored or compared in plain text. Uses unique randomized salts.
- **Hardware Integration:** Bundles Apache POI for extensive Excel data migrations without requiring Microsoft Office installations.