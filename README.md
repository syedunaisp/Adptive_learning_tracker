# How to Run the Adaptive Learning Progress Tracker

## Prerequisites
- Java Development Kit (JDK) 8 or higher installed.
- `javac` and `java` commands should be available in your system PATH.

## Commands

### 1. Open Terminal in Project Root
Navigate to the folder containing `src` and `bin`.

### 2. Compile the Code
Run the following command to compile the Java source files into the `bin` directory:

```powershell
javac -d bin -sourcepath src src/tracker/Main.java
```

### 3. Run the Application
Once compiled, start the application with:

```powershell
java -cp bin tracker.Main
```

## Troubleshooting
If you see an error like `'javac' is not recognized`, you may need to use the full path to your JDK installation (e.g., `& "C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\javac.exe" ...`).
