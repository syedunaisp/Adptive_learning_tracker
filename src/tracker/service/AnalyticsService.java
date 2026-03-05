package tracker.service;

import tracker.model.*;
import tracker.model.RiskScore.Level;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;

import java.util.*;

/**
 * Service layer for institutional-level analytics.
 *
 * Provides aggregated statistics across all students:
 *   - Risk distribution (count per level)
 *   - Most common weak subject
 *   - Institutional average score
 *   - Highest/lowest performers
 *   - Category-level insights
 *
 * Keeps all analytics logic OUT of the UI layer.
 */
public class AnalyticsService {

    private final RiskPredictor riskPredictor;
    private final TrendAnalyzer trendAnalyzer;

    public AnalyticsService(RiskPredictor riskPredictor, TrendAnalyzer trendAnalyzer) {
        this.riskPredictor = riskPredictor;
        this.trendAnalyzer = trendAnalyzer;
    }

    // ==========================================
    // RISK DISTRIBUTION
    // ==========================================

    /**
     * Returns the count of students at each risk level.
     *
     * @param students list of all students
     * @return map of Level -> count
     */
    public Map<Level, Integer> getRiskDistribution(List<Student> students) {
        Map<Level, Integer> distribution = new EnumMap<>(Level.class);
        for (Level l : Level.values()) {
            distribution.put(l, 0);
        }
        for (Student s : students) {
            if (s.getSubjects().isEmpty()) continue;
            RiskScore risk = riskPredictor.assessRisk(s);
            distribution.merge(risk.getLevel(), 1, Integer::sum);
        }
        return distribution;
    }

    // ==========================================
    // MOST COMMON WEAK SUBJECT
    // ==========================================

    /**
     * Finds the most frequently occurring weak subject across all students.
     *
     * @param students list of all students
     * @return the subject name, or "None" if no weak subjects exist
     */
    public String getMostCommonWeakSubject(List<Student> students) {
        Map<String, Integer> frequency = new HashMap<>();
        for (Student s : students) {
            for (Subject sub : s.getWeakSubjects()) {
                String key = sub.getSubjectName().trim().toLowerCase();
                frequency.merge(key, 1, Integer::sum);
            }
        }
        if (frequency.isEmpty()) return "None";

        return Collections.max(frequency.entrySet(),
                Map.Entry.comparingByValue()).getKey();
    }

    // ==========================================
    // INSTITUTIONAL AVERAGE
    // ==========================================

    /**
     * Calculates the institutional average score across all students.
     *
     * @param students list of all students
     * @return the institution-wide average, or 0.0 if no data
     */
    public double getInstitutionalAverage(List<Student> students) {
        if (students == null || students.isEmpty()) return 0.0;

        double total = 0;
        int count = 0;
        for (Student s : students) {
            if (!s.getSubjects().isEmpty()) {
                total += s.getAverageScore();
                count++;
            }
        }
        return count > 0 ? total / count : 0.0;
    }

    // ==========================================
    // TOP / BOTTOM PERFORMERS
    // ==========================================

    /**
     * Returns the student with the highest average score.
     *
     * @param students list of all students
     * @return the top performer, or null if empty
     */
    public Student getHighestPerformer(List<Student> students) {
        Student best = null;
        double bestAvg = -1;
        for (Student s : students) {
            if (!s.getSubjects().isEmpty() && s.getAverageScore() > bestAvg) {
                bestAvg = s.getAverageScore();
                best = s;
            }
        }
        return best;
    }

    /**
     * Returns the student with the lowest average score.
     *
     * @param students list of all students
     * @return the lowest performer, or null if empty
     */
    public Student getLowestPerformer(List<Student> students) {
        Student worst = null;
        double worstAvg = Double.MAX_VALUE;
        for (Student s : students) {
            if (!s.getSubjects().isEmpty() && s.getAverageScore() < worstAvg) {
                worstAvg = s.getAverageScore();
                worst = s;
            }
        }
        return worst;
    }

    // ==========================================
    // CATEGORY INSIGHTS
    // ==========================================

    /**
     * Returns the average score per subject category across all students.
     *
     * @param students list of all students
     * @return map of SubjectCategory -> average score
     */
    public Map<SubjectCategory, Double> getCategoryAverages(List<Student> students) {
        Map<SubjectCategory, Double> totals = new EnumMap<>(SubjectCategory.class);
        Map<SubjectCategory, Integer> counts = new EnumMap<>(SubjectCategory.class);

        for (Student s : students) {
            for (Subject sub : s.getSubjects()) {
                SubjectCategory cat = SubjectCategory.categorize(sub.getSubjectName());
                totals.merge(cat, sub.getScore(), Double::sum);
                counts.merge(cat, 1, Integer::sum);
            }
        }

        Map<SubjectCategory, Double> averages = new EnumMap<>(SubjectCategory.class);
        for (Map.Entry<SubjectCategory, Double> entry : totals.entrySet()) {
            int c = counts.getOrDefault(entry.getKey(), 1);
            averages.put(entry.getKey(), entry.getValue() / c);
        }
        return averages;
    }

    /**
     * Identifies the weakest category institution-wide.
     *
     * @param students list of all students
     * @return the weakest SubjectCategory, or UNCATEGORIZED if no data
     */
    public SubjectCategory getWeakestCategory(List<Student> students) {
        Map<SubjectCategory, Double> avgs = getCategoryAverages(students);
        SubjectCategory weakest = SubjectCategory.UNCATEGORIZED;
        double lowestAvg = Double.MAX_VALUE;

        for (Map.Entry<SubjectCategory, Double> entry : avgs.entrySet()) {
            if (entry.getKey() == SubjectCategory.UNCATEGORIZED) continue;
            if (entry.getValue() < lowestAvg) {
                lowestAvg = entry.getValue();
                weakest = entry.getKey();
            }
        }
        return weakest;
    }

    /**
     * Generates a formatted analytics summary string for display.
     *
     * @param students list of all students
     * @return multi-line formatted analytics report
     */
    public String generateAnalyticsSummary(List<Student> students) {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================\n");
        sb.append("    INSTITUTIONAL ANALYTICS DASHBOARD\n");
        sb.append("============================================\n\n");

        // --- Risk Distribution ---
        Map<Level, Integer> riskDist = getRiskDistribution(students);
        sb.append("--- Risk Distribution ---\n");
        for (Level l : Level.values()) {
            int count = riskDist.getOrDefault(l, 0);
            sb.append(String.format("  %-15s : %d student(s)\n", l.getLabel(), count));
        }
        sb.append("\n");

        // --- Institutional Average ---
        double instAvg = getInstitutionalAverage(students);
        sb.append(String.format("Institutional Average : %.2f\n", instAvg));
        sb.append(String.format("Total Students        : %d\n\n", students.size()));

        // --- Top/Bottom Performers ---
        Student top = getHighestPerformer(students);
        Student bottom = getLowestPerformer(students);
        sb.append("--- Performance Extremes ---\n");
        if (top != null) {
            sb.append(String.format("  Highest: %s (%s) - Avg: %.2f\n",
                    top.getName(), top.getId(), top.getAverageScore()));
        }
        if (bottom != null) {
            sb.append(String.format("  Lowest : %s (%s) - Avg: %.2f\n",
                    bottom.getName(), bottom.getId(), bottom.getAverageScore()));
        }
        sb.append("\n");

        // --- Most Common Weak Subject ---
        String commonWeak = getMostCommonWeakSubject(students);
        sb.append("Most Common Weak Subject: ").append(commonWeak).append("\n\n");

        // --- Category Averages ---
        Map<SubjectCategory, Double> catAvgs = getCategoryAverages(students);
        if (!catAvgs.isEmpty()) {
            sb.append("--- Category Averages ---\n");
            for (Map.Entry<SubjectCategory, Double> entry : catAvgs.entrySet()) {
                sb.append(String.format("  %-35s : %.2f\n",
                        entry.getKey().getDisplayName(), entry.getValue()));
            }
            SubjectCategory weakestCat = getWeakestCategory(students);
            if (weakestCat != SubjectCategory.UNCATEGORIZED) {
                sb.append("\n  Weakest Category: ").append(weakestCat.getDisplayName()).append("\n");
            }
        }

        sb.append("\n============================================\n");
        return sb.toString();
    }
}
