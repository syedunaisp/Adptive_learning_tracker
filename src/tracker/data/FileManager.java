package tracker.data;

import tracker.model.Student;
import tracker.model.Subject;
import tracker.model.RiskScore;
import tracker.model.TrendDirection;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Handles file I/O operations for saving student academic reports.
 * Writes to "academic_report.txt" in append mode using FileWriter + BufferedWriter.
 *
 * UPGRADED: Now supports enhanced reports with risk breakdown,
 * trend indicators, adaptive recommendations, and bulk export.
 */
public class FileManager {

    private static final String REPORT_FILE = "academic_report.txt";
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Saves an enhanced academic report for a single student.
     * Includes risk score breakdown, trend indicator, and adaptive recommendations.
     *
     * @param student         the student whose report is being saved
     * @param risk            the computed risk score (may be null for backward compat)
     * @param trend           the trend direction (may be null for backward compat)
     * @param recommendations the list of recommendation strings
     * @throws IOException if file writing fails
     */
    public void saveEnhancedReport(Student student, RiskScore risk,
                                    TrendDirection trend,
                                    List<String> recommendations) throws IOException {
        if (student == null) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILE, true))) {
            writeEnhancedReport(writer, student, risk, trend, recommendations);
            writer.flush();
        }
    }

    /**
     * Backward-compatible method: saves a basic report without risk/trend data.
     * Delegates to the enhanced report with null risk/trend.
     *
     * @param student         the student whose report is being saved
     * @param recommendations the list of recommendation strings
     * @throws IOException if file writing fails
     */
    public void saveReport(Student student, List<String> recommendations) throws IOException {
        saveEnhancedReport(student, null, null, recommendations);
    }

    /**
     * Bulk export: saves enhanced reports for all students in a single file operation.
     *
     * @param students        list of students
     * @param risks           list of risk scores (parallel to students)
     * @param trends          list of trend directions (parallel to students)
     * @param allRecommendations list of recommendation lists (parallel to students)
     * @throws IOException if file writing fails
     */
    public void saveBulkReport(List<Student> students,
                                List<RiskScore> risks,
                                List<TrendDirection> trends,
                                List<List<String>> allRecommendations) throws IOException {
        if (students == null || students.isEmpty()) return;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(REPORT_FILE, true))) {
            writer.write("################################################################");
            writer.newLine();
            writer.write("          BULK ACADEMIC REPORT EXPORT");
            writer.newLine();
            writer.write("          Generated: " + LocalDateTime.now().format(DATE_FORMAT));
            writer.newLine();
            writer.write("          Total Students: " + students.size());
            writer.newLine();
            writer.write("################################################################");
            writer.newLine();
            writer.newLine();

            for (int i = 0; i < students.size(); i++) {
                Student student = students.get(i);
                RiskScore risk = (risks != null && i < risks.size()) ? risks.get(i) : null;
                TrendDirection trend = (trends != null && i < trends.size()) ? trends.get(i) : null;
                List<String> recs = (allRecommendations != null && i < allRecommendations.size())
                        ? allRecommendations.get(i) : null;

                writeEnhancedReport(writer, student, risk, trend, recs);
            }

            writer.flush();
        }
    }

    /**
     * Internal: writes a single enhanced report block to the writer.
     */
    private void writeEnhancedReport(BufferedWriter writer, Student student,
                                      RiskScore risk, TrendDirection trend,
                                      List<String> recommendations) throws IOException {
        writer.write("============================================");
        writer.newLine();
        writer.write("        ACADEMIC INTELLIGENCE REPORT");
        writer.newLine();
        writer.write("============================================");
        writer.newLine();

        // Student Info
        writer.write("Student Name : " + student.getName());
        writer.newLine();
        writer.write("Student ID   : " + student.getId());
        writer.newLine();
        writer.write("Average Score: " + String.format("%.2f", student.getAverageScore()));
        writer.newLine();

        // Risk Assessment (enhanced)
        if (risk != null) {
            writer.write("Risk Level   : " + risk.getLevel().getLabel());
            writer.newLine();
            writer.write("Risk Score   : " + String.format("%.1f / 100", risk.getNumericScore()));
            writer.newLine();
        } else {
            // Backward compat: simple threshold
            String status = student.isAtRisk() ? "AT RISK" : "Normal";
            writer.write("Status       : " + status);
            writer.newLine();
        }

        // Trend Indicator
        if (trend != null) {
            writer.write("Trend        : " + trend.getLabel() + " (" + trend.getArrow() + ")");
            writer.newLine();
        }

        // Subject Scores
        writer.write("--------------------------------------------");
        writer.newLine();
        writer.write("Subjects:");
        writer.newLine();
        for (Subject sub : student.getSubjects()) {
            String marker = sub.getScore() < 60 ? " [WEAK]" : "";
            writer.write(String.format("  %-20s : %6.2f%s",
                    sub.getSubjectName(), sub.getScore(), marker));
            writer.newLine();
        }

        // Weak Subjects
        writer.write("--------------------------------------------");
        writer.newLine();
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

        // Risk Breakdown (if available)
        if (risk != null) {
            writer.write("--------------------------------------------");
            writer.newLine();
            writer.write("Risk Score Breakdown:");
            writer.newLine();
            for (String explanation : risk.getExplanations()) {
                writer.write("  " + explanation);
                writer.newLine();
            }
        }

        // Recommendations
        writer.write("--------------------------------------------");
        writer.newLine();
        writer.write("Recommendations:");
        writer.newLine();
        if (recommendations == null || recommendations.isEmpty()) {
            writer.write("  - No specific recommendations at this time.");
            writer.newLine();
        } else {
            for (String rec : recommendations) {
                writer.write("  " + rec);
                writer.newLine();
            }
        }

        // Timestamp
        writer.write("--------------------------------------------");
        writer.newLine();
        writer.write("Timestamp    : " + LocalDateTime.now().format(DATE_FORMAT));
        writer.newLine();

        writer.write("============================================");
        writer.newLine();
        writer.newLine();
    }
}
