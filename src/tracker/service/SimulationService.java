package tracker.service;

import tracker.model.*;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * Risk simulation service implementing "what-if" analysis.
 *
 * Allows testing hypothetical score changes without modifying
 * the actual student data. Works by:
 *   1. Deep-cloning the student's subject list
 *   2. Applying the hypothetical modification
 *   3. Recalculating average, risk, and recommendations
 *   4. Returning a {@link SimulationResult} with before/after comparison
 *
 * The original Student object is NEVER modified.
 */
public class SimulationService {

    private final RiskPredictor riskPredictor;
    private final TrendAnalyzer trendAnalyzer;
    private final tracker.service.ai.AdaptivePlanner planner;

    public SimulationService(RiskPredictor riskPredictor,
                             TrendAnalyzer trendAnalyzer,
                             tracker.service.ai.AdaptivePlanner planner) {
        this.riskPredictor = riskPredictor;
        this.trendAnalyzer = trendAnalyzer;
        this.planner = planner;
    }

    /**
     * Simulates the effect of changing a specific subject's score by a delta.
     *
     * Example: "What if Math improves by +10?"
     *
     * @param student     the original student (NOT modified)
     * @param subjectName the subject to adjust (case-insensitive match)
     * @param scoreDelta  the score change to apply (can be negative)
     * @return a SimulationResult comparing before and after
     * @throws IllegalArgumentException if the subject is not found
     */
    public SimulationResult simulateScoreChange(Student student,
                                                 String subjectName,
                                                 double scoreDelta) {
        if (student == null || subjectName == null) {
            throw new IllegalArgumentException("Student and subject name must not be null.");
        }

        // --- Clone subject list ---
        List<Subject> clonedSubjects = deepCloneSubjects(student.getSubjects());

        // --- Find and modify the target subject in the clone ---
        boolean found = false;
        for (Subject s : clonedSubjects) {
            if (s.getSubjectName().equalsIgnoreCase(subjectName.trim())) {
                double newScore = Math.max(0, Math.min(100, s.getScore() + scoreDelta));
                s.setScore(newScore);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new IllegalArgumentException(
                    "Subject '" + subjectName + "' not found for student '" + student.getName() + "'.");
        }

        // --- Compute original risk ---
        RiskScore originalRisk = riskPredictor.assessRisk(student);
        double originalAvg = student.getAverageScore();

        // --- Compute simulated metrics ---
        double simAvg = calculateAverage(clonedSubjects);
        List<Subject> simWeak = getWeakSubjects(clonedSubjects);
        double simLowest = findLowestScore(clonedSubjects);
        TrendDirection trend = trendAnalyzer.getTrend(student.getId());

        RiskScore simRisk = riskPredictor.assessRisk(
                simAvg, simWeak.size(), simLowest,
                clonedSubjects.size(), trend);

        // --- Build a temporary student for recommendation generation ---
        Student simStudent = new Student(student.getId(), student.getName());
        simStudent.setSubjects(clonedSubjects);

        List<String> simRecommendations = planner.generateRecommendations(
                simStudent, simRisk, trend);

        // --- Build scenario description ---
        String scenario = String.format("%s %+.1f points",
                subjectName, scoreDelta);

        return new SimulationResult(
                student.getId(), student.getName(), scenario,
                originalAvg, simAvg,
                originalRisk, simRisk,
                simRecommendations);
    }

    // --- Helper methods ---

    private List<Subject> deepCloneSubjects(List<Subject> original) {
        List<Subject> cloned = new ArrayList<>();
        for (Subject s : original) {
            cloned.add(new Subject(s.getSubjectName(), s.getScore()));
        }
        return cloned;
    }

    private double calculateAverage(List<Subject> subjects) {
        if (subjects.isEmpty()) return 0.0;
        double total = 0;
        for (Subject s : subjects) total += s.getScore();
        return total / subjects.size();
    }

    private List<Subject> getWeakSubjects(List<Subject> subjects) {
        List<Subject> weak = new ArrayList<>();
        for (Subject s : subjects) {
            if (s.getScore() < 60) weak.add(s);
        }
        return weak;
    }

    private double findLowestScore(List<Subject> subjects) {
        double lowest = Double.MAX_VALUE;
        for (Subject s : subjects) {
            if (s.getScore() < lowest) lowest = s.getScore();
        }
        return lowest == Double.MAX_VALUE ? 0 : lowest;
    }
}
