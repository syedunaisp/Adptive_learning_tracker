package tracker.data;

import tracker.model.Student;
import tracker.model.Subject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles file I/O operations for saving student academic reports.
 * Writes to "academic_report.txt" in append mode using FileWriter + BufferedWriter.
 */
public class FileManager {

    private static final String REPORT_FILE = "academic_report.txt";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Saves an academic report for a single student to the report file.
     * Appends a formatted block containing name, average, weak subjects,
     * recommendations, and a timestamp.
     *
     * @param student         the student whose report is being saved
     * @param recommendations the list of recommendation strings
     * @throws IOException if file writing fails
     */
    public void saveReport(Student student, List<String> recommendations) throws IOException {
        if (student == null) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILE, true))) {
            writer.write("============================================");
            writer.newLine();
            writer.write("        ACADEMIC REPORT");
            writer.newLine();
            writer.write("============================================");
            writer.newLine();

            // Student Name
            writer.write("Student Name : " + student.getName());
            writer.newLine();

            // Student ID
            writer.write("Student ID   : " + student.getId());
            writer.newLine();

            // Average Score
            writer.write("Average Score: " + String.format("%.2f", student.getAverageScore()));
            writer.newLine();

            // Status
            String status = student.isAtRisk() ? "AT RISK" : "Normal";
            writer.write("Status       : " + status);
            writer.newLine();

            // Weak Subjects
            writer.write("Weak Subjects: ");
            List<Subject> weakSubjects = student.getWeakSubjects();
            if (weakSubjects.isEmpty()) {
                writer.write("None");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < weakSubjects.size(); i++) {
                    sb.append(weakSubjects.get(i).getSubjectName());
                    sb.append(" (").append(weakSubjects.get(i).getScore()).append(")");
                    if (i < weakSubjects.size() - 1) {
                        sb.append(", ");
                    }
                }
                writer.write(sb.toString());
            }
            writer.newLine();

            // Recommendations
            writer.write("Recommendations:");
            writer.newLine();
            if (recommendations == null || recommendations.isEmpty()) {
                writer.write("  - No specific recommendations at this time.");
                writer.newLine();
            } else {
                for (String rec : recommendations) {
                    writer.write("  - " + rec);
                    writer.newLine();
                }
            }

            // Timestamp
            writer.write("Timestamp    : " + LocalDateTime.now().format(DATE_FORMAT));
            writer.newLine();

            writer.write("============================================");
            writer.newLine();
            writer.newLine();

            writer.flush();
        }
    }
}
