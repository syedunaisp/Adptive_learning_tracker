package tracker.service.ai;

import tracker.model.TrendDirection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks performance history and detects trends for each student.
 *
 * Stores the previous average score for each student (keyed by ID).
 * When a new average is recorded, compares it against the stored
 * value to classify the trend as IMPROVING, STABLE, or DECLINING.
 *
 * V2 Enhancement: Also maintains full score history per student for
 * simple linear regression-based trajectory prediction.
 *
 * Threshold: a change of +/- 2.0 points is considered significant.
 * Anything within that band is classified as STABLE.
 *
 * Thread-safe: all access to the history map is synchronized.
 */
public class TrendAnalyzer {

    /** Minimum delta to classify as improving or declining. */
    private static final double SIGNIFICANCE_THRESHOLD = 2.0;

    /**
     * Maps student ID -> their previously recorded average score.
     * Null or absent means no history exists yet (first entry).
     */
    private final Map<String, Double> previousAverages;

    /**
     * Cached trend direction per student, updated on each recordAverage call.
     */
    private final Map<String, TrendDirection> currentTrends;

    /**
     * V2: Full score history per student for regression analysis.
     */
    private final Map<String, List<Double>> scoreHistory;

    public TrendAnalyzer() {
        this.previousAverages = new HashMap<>();
        this.currentTrends = new HashMap<>();
        this.scoreHistory = new HashMap<>();
    }

    /**
     * Records a new average score for the student and updates the trend.
     *
     * Call this AFTER the student's subjects have been updated so that
     * Student.getAverageScore() returns the new value.
     *
     * @param studentId  the student's unique ID
     * @param newAverage the student's current average score
     */
    public synchronized void recordAverage(String studentId, double newAverage) {
        if (studentId == null)
            return;

        Double previous = previousAverages.get(studentId);

        if (previous == null) {
            // First record -- no trend data yet; default to STABLE
            currentTrends.put(studentId, TrendDirection.STABLE);
        } else {
            double delta = newAverage - previous;
            if (delta > SIGNIFICANCE_THRESHOLD) {
                currentTrends.put(studentId, TrendDirection.IMPROVING);
            } else if (delta < -SIGNIFICANCE_THRESHOLD) {
                currentTrends.put(studentId, TrendDirection.DECLINING);
            } else {
                currentTrends.put(studentId, TrendDirection.STABLE);
            }
        }

        // Store new average as the baseline for next comparison
        previousAverages.put(studentId, newAverage);

        // V2: Append to score history
        scoreHistory.computeIfAbsent(studentId, k -> new ArrayList<>()).add(newAverage);
    }

    /**
     * Returns the most recently computed trend for the student.
     *
     * @param studentId the student's unique ID
     * @return the trend direction, or STABLE if no data exists
     */
    public synchronized TrendDirection getTrend(String studentId) {
        TrendDirection trend = currentTrends.get(studentId);
        return trend != null ? trend : TrendDirection.STABLE;
    }

    /**
     * Returns the previously recorded average, or null if none exists.
     *
     * @param studentId the student's unique ID
     * @return the previous average, or null
     */
    public synchronized Double getPreviousAverage(String studentId) {
        return previousAverages.get(studentId);
    }

    /**
     * Checks whether any history exists for the student.
     *
     * @param studentId the student's unique ID
     * @return true if at least one average has been recorded
     */
    public synchronized boolean hasHistory(String studentId) {
        return previousAverages.containsKey(studentId);
    }

    /**
     * Clears all trend data. Used for testing or data reset.
     */
    public synchronized void clearAll() {
        previousAverages.clear();
        currentTrends.clear();
        scoreHistory.clear();
    }

    // =========================================================================
    // V2: Linear Regression & Trajectory Prediction
    // =========================================================================

    /**
     * Predicts a future score using simple linear regression.
     *
     * Uses the least-squares method:
     * slope m = (n*Σxy - Σx*Σy) / (n*Σx² - (Σx)²)
     * intercept b = (Σy - m*Σx) / n
     * prediction = m * (n-1 + periodsAhead) + b
     *
     * @param studentId    the student's unique ID
     * @param periodsAhead number of periods into the future to predict
     * @return the predicted score, clamped to [0, 100]
     */
    public synchronized double predictFutureScore(String studentId, int periodsAhead) {
        List<Double> history = scoreHistory.get(studentId);
        if (history == null || history.isEmpty()) {
            return 0.0;
        }
        if (history.size() < 2) {
            return history.get(history.size() - 1);
        }

        int n = history.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = i;
            double y = history.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            // All x values the same (shouldn't happen with indices), return last
            return history.get(n - 1);
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        double futureX = (n - 1) + periodsAhead;
        double predicted = slope * futureX + intercept;

        // Clamp to valid score range
        return Math.max(0, Math.min(100, predicted));
    }

    /**
     * Returns a read-only copy of the full score history for a student.
     *
     * @param studentId the student's unique ID
     * @return list of historical scores, or empty list if no history
     */
    public synchronized List<Double> getScoreHistory(String studentId) {
        List<Double> history = scoreHistory.get(studentId);
        if (history == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    /**
     * Returns a human-readable projection string.
     *
     * @param studentId the student's unique ID
     * @return projection text, e.g., "Projected to reach 75.3 in next period"
     */
    public synchronized String getProjectedTrend(String studentId) {
        List<Double> history = scoreHistory.get(studentId);
        if (history == null || history.size() < 2) {
            return "Insufficient data for projection (need at least 2 data points).";
        }

        double predicted = predictFutureScore(studentId, 1);
        double current = history.get(history.size() - 1);
        double delta = predicted - current;
        String direction = delta > 0 ? "↑" : (delta < 0 ? "↓" : "→");

        return String.format("Projected to reach %.1f in next period (%s %.1f from current %.1f)",
                predicted, direction, Math.abs(delta), current);
    }
}
