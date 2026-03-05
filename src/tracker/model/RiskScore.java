package tracker.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a weighted risk assessment result for a student.
 *
 * Components of the score (0-100 scale):
 * 1. Average score contribution (weight 0.35)
 * 2. Weak subject count (weight 0.25)
 * 3. Lowest score severity (weight 0.25)
 * 4. Trend direction penalty/bonus (weight 0.15)
 *
 * A higher numeric score means HIGHER risk.
 */
public class RiskScore {

    /** Risk classification levels. */
    public enum Level {
        LOW("Low Risk"),
        MODERATE("Moderate Risk"),
        HIGH("High Risk");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // --- Weights (must sum to 1.0) ---
    public static final double W_AVERAGE = 0.35;
    public static final double W_WEAK_COUNT = 0.25;
    public static final double W_LOWEST = 0.25;
    public static final double W_TREND = 0.15;

    // --- Thresholds ---
    public static final double THRESHOLD_HIGH = 60.0;
    public static final double THRESHOLD_MODERATE = 30.0;

    private final double numericScore;
    private final Level level;
    private final List<String> explanations;

    // Component scores for transparency
    private final double averageComponent;
    private final double weakCountComponent;
    private final double lowestComponent;
    private final double trendComponent;

    /**
     * Constructs a RiskScore by computing weighted components.
     *
     * @param averageScore     the student's current average (0-100)
     * @param weakSubjectCount number of subjects scoring below 60
     * @param lowestScore      the student's lowest individual score
     * @param totalSubjects    total number of subjects enrolled
     * @param trend            the student's performance trend direction
     */
    public RiskScore(double averageScore, int weakSubjectCount,
            double lowestScore, int totalSubjects,
            TrendDirection trend) {

        List<String> reasons = new ArrayList<>();

        // --- Component 1: Average Score Risk (0-100) ---
        // Lower average = higher risk
        double avgRisk = Math.max(0, 100 - averageScore);
        this.averageComponent = avgRisk * W_AVERAGE;
        reasons.add(String.format("Average Score Risk: %.1f/100 -> weighted %.1f (avg=%.1f, weight=%.0f%%)",
                avgRisk, averageComponent, averageScore, W_AVERAGE * 100));

        // --- Component 2: Weak Subject Density (0-100) ---
        double weakRatio = totalSubjects > 0
                ? (double) weakSubjectCount / totalSubjects * 100.0
                : 0;
        this.weakCountComponent = weakRatio * W_WEAK_COUNT;
        reasons.add(String.format("Weak Subject Density: %.1f%% (%d/%d weak) -> weighted %.1f (weight=%.0f%%)",
                weakRatio, weakSubjectCount, totalSubjects, weakCountComponent, W_WEAK_COUNT * 100));

        // --- Component 3: Lowest Score Severity (0-100) ---
        double lowestRisk = Math.max(0, 100 - lowestScore);
        this.lowestComponent = lowestRisk * W_LOWEST;
        reasons.add(String.format("Lowest Score Severity: %.1f/100 -> weighted %.1f (lowest=%.1f, weight=%.0f%%)",
                lowestRisk, lowestComponent, lowestScore, W_LOWEST * 100));

        // --- Component 4: Trend Modifier (0-100) ---
        double trendRisk;
        switch (trend) {
            case DECLINING:
                trendRisk = 80.0;
                reasons.add(String.format("Trend Penalty: DECLINING -> %.1f/100, weighted %.1f (weight=%.0f%%)",
                        trendRisk, trendRisk * W_TREND, W_TREND * 100));
                break;
            case STABLE:
                trendRisk = 40.0;
                reasons.add(String.format("Trend Neutral: STABLE -> %.1f/100, weighted %.1f (weight=%.0f%%)",
                        trendRisk, trendRisk * W_TREND, W_TREND * 100));
                break;
            case IMPROVING:
            default:
                trendRisk = 10.0;
                reasons.add(String.format("Trend Bonus: IMPROVING -> %.1f/100, weighted %.1f (weight=%.0f%%)",
                        trendRisk, trendRisk * W_TREND, W_TREND * 100));
                break;
        }
        this.trendComponent = trendRisk * W_TREND;

        // --- Final Score ---
        this.numericScore = averageComponent + weakCountComponent
                + lowestComponent + trendComponent;

        // --- Classify ---
        if (numericScore >= THRESHOLD_HIGH) {
            this.level = Level.HIGH;
        } else if (numericScore >= THRESHOLD_MODERATE) {
            this.level = Level.MODERATE;
        } else {
            this.level = Level.LOW;
        }

        reasons.add(String.format("TOTAL RISK SCORE: %.1f -> %s (thresholds: High>=%.0f, Moderate>=%.0f)",
                numericScore, level.getLabel(), THRESHOLD_HIGH, THRESHOLD_MODERATE));

        this.explanations = Collections.unmodifiableList(reasons);
    }

    public double getNumericScore() {
        return numericScore;
    }

    public Level getLevel() {
        return level;
    }

    public List<String> getExplanations() {
        return explanations;
    }

    public double getAverageComponent() {
        return averageComponent;
    }

    public double getWeakCountComponent() {
        return weakCountComponent;
    }

    public double getLowestComponent() {
        return lowestComponent;
    }

    public double getTrendComponent() {
        return trendComponent;
    }

    /**
     * Returns the percentage contribution of the average score factor to total
     * risk.
     */
    public double getAverageContributionPercent() {
        return numericScore > 0 ? (averageComponent / numericScore) * 100.0 : 0;
    }

    /**
     * Returns the percentage contribution of the weak subject count factor to total
     * risk.
     */
    public double getWeakCountContributionPercent() {
        return numericScore > 0 ? (weakCountComponent / numericScore) * 100.0 : 0;
    }

    /**
     * Returns the percentage contribution of the lowest score factor to total risk.
     */
    public double getLowestContributionPercent() {
        return numericScore > 0 ? (lowestComponent / numericScore) * 100.0 : 0;
    }

    /** Returns the percentage contribution of the trend factor to total risk. */
    public double getTrendContributionPercent() {
        return numericScore > 0 ? (trendComponent / numericScore) * 100.0 : 0;
    }

    /**
     * Returns a concise single-line summary.
     */
    public String getSummary() {
        return String.format("%s (Score: %.1f/100)", level.getLabel(), numericScore);
    }

    /**
     * Returns a full multi-line breakdown.
     */
    public String getDetailedBreakdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Risk Score Breakdown ===\n");
        for (String line : explanations) {
            sb.append("  ").append(line).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
