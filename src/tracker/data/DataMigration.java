package tracker.data;

import tracker.data.dao.StudentDAO;
import tracker.data.dao.SubjectDAO;
import tracker.data.dao.ScoreDAO;
import tracker.model.SubjectCategory;

import java.io.*;
import java.util.*;

/**
 * Utility to migrate data from the legacy academic_report.txt format
 * into the SQLite database.
 *
 * Parses the structured report format written by FileManager and inserts
 * student/subject/score records via the DAO layer.
 *
 * Handles duplicate detection: if a student already exists in the DB,
 * it skips re-inserting but still imports any new subject scores.
 *
 * Usage:
 *   String result = DataMigration.migrateFromReportFile();
 *   // or
 *   String result = DataMigration.migrateFromReportFile("path/to/report.txt");
 */
public class DataMigration {

    private static final String DEFAULT_REPORT_FILE = "academic_report.txt";

    /**
     * Migrates data from the default report file (academic_report.txt).
     *
     * @return a summary string describing the migration results
     */
    public static String migrateFromReportFile() {
        return migrateFromReportFile(DEFAULT_REPORT_FILE);
    }

    /**
     * Migrates data from a specified report file.
     *
     * Expected format (per student block):
     * <pre>
     * ============================================
     *         ACADEMIC INTELLIGENCE REPORT
     * ============================================
     * Student Name : John Doe
     * Student ID   : S001
     * Average Score: 65.50
     * ...
     * Subjects:
     *   Math                 :  70.00
     *   Physics              :  55.00 [WEAK]
     *   English              :  71.50
     * --------------------------------------------
     * ...
     * </pre>
     *
     * @param filePath path to the report file
     * @return a summary string describing the migration results
     */
    public static String migrateFromReportFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "Migration: No file found at '" + filePath + "'.\nNothing to migrate.";
        }

        StudentDAO studentDAO = new StudentDAO();
        SubjectDAO subjectDAO = new SubjectDAO();
        ScoreDAO scoreDAO = new ScoreDAO();

        int studentsCreated = 0;
        int studentsSkipped = 0;
        int scoresImported = 0;
        int linesParsed = 0;
        List<String> errors = new ArrayList<>();
        Set<String> seenStudentIds = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String currentId = null;
            String currentName = null;
            boolean inSubjectsBlock = false;

            while ((line = reader.readLine()) != null) {
                linesParsed++;
                String trimmed = line.trim();

                // Detect student ID
                if (trimmed.startsWith("Student ID")) {
                    currentId = extractValue(trimmed);
                    inSubjectsBlock = false;
                }
                // Detect student name
                else if (trimmed.startsWith("Student Name")) {
                    currentName = extractValue(trimmed);
                    inSubjectsBlock = false;
                }
                // Detect start of subjects section
                else if (trimmed.equals("Subjects:")) {
                    inSubjectsBlock = true;
                }
                // Detect section separator — end of subjects block
                else if (trimmed.startsWith("----") || trimmed.startsWith("====")) {
                    inSubjectsBlock = false;
                }
                // Parse subject lines within the subjects block
                else if (inSubjectsBlock && currentId != null && currentName != null) {
                    int colonIdx = trimmed.indexOf(':');
                    if (colonIdx > 0 && colonIdx < trimmed.length() - 1) {
                        String subjectName = trimmed.substring(0, colonIdx).trim();
                        String scoreStr = trimmed.substring(colonIdx + 1).trim()
                                .replaceAll("\\[WEAK\\]", "")
                                .replaceAll("\\[weak\\]", "")
                                .trim();

                        // Skip non-subject lines that might have colons
                        if (subjectName.isEmpty() || subjectName.contains("=")) continue;

                        try {
                            double score = Double.parseDouble(scoreStr);
                            if (score < 0 || score > 100) {
                                errors.add("Line " + linesParsed + ": Score out of range: " + trimmed);
                                continue;
                            }

                            // Ensure student exists in DB
                            if (!seenStudentIds.contains(currentId)) {
                                if (!studentDAO.existsByStudentId(currentId)) {
                                    studentDAO.insert(currentId, currentName);
                                    studentsCreated++;
                                } else {
                                    studentsSkipped++;
                                }
                                seenStudentIds.add(currentId);
                            }

                            int studentDbId = studentDAO.findDbIdByStudentId(currentId);
                            if (studentDbId < 0) {
                                errors.add("Line " + linesParsed + ": Could not resolve student DB ID for " + currentId);
                                continue;
                            }

                            // Ensure subject exists in DB
                            String category = SubjectCategory.categorize(subjectName).name();
                            int subjectDbId = subjectDAO.findOrCreate(subjectName, category);
                            if (subjectDbId < 0) {
                                errors.add("Line " + linesParsed + ": Could not create subject: " + subjectName);
                                continue;
                            }

                            // Insert score
                            scoreDAO.insertScore(studentDbId, subjectDbId, score);
                            scoresImported++;

                        } catch (NumberFormatException e) {
                            // Not a score line — skip silently (could be a recommendation or other text)
                        }
                    }
                }

                // Reset state at end of a report block (double separator)
                if (trimmed.startsWith("====") && trimmed.length() > 10) {
                    // Keep currentId and currentName until next block starts
                }
            }
        } catch (IOException e) {
            return "Migration Error: Could not read file '" + filePath + "'.\n" + e.getMessage();
        }

        // Build summary
        StringBuilder sb = new StringBuilder();
        sb.append("=== Data Migration Summary ===\n");
        sb.append("Source file:       ").append(filePath).append("\n");
        sb.append("Lines parsed:      ").append(linesParsed).append("\n");
        sb.append("Students created:  ").append(studentsCreated).append("\n");
        sb.append("Students skipped:  ").append(studentsSkipped).append(" (already in DB)\n");
        sb.append("Scores imported:   ").append(scoresImported).append("\n");

        if (!errors.isEmpty()) {
            sb.append("\nWarnings/Errors (").append(errors.size()).append("):\n");
            for (String err : errors) {
                sb.append("  - ").append(err).append("\n");
            }
        } else {
            sb.append("\nNo errors.\n");
        }

        if (studentsCreated == 0 && scoresImported == 0) {
            sb.append("\nNo new data was imported. The file may be empty or all data already exists in the database.");
        }

        return sb.toString();
    }

    /**
     * Extracts the value after the first colon in a line.
     * e.g. "Student ID   : S001" returns "S001"
     */
    private static String extractValue(String line) {
        int colonIdx = line.indexOf(':');
        if (colonIdx < 0 || colonIdx >= line.length() - 1) return "";
        return line.substring(colonIdx + 1).trim();
    }
}
