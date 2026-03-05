package tracker.service.ai;

import tracker.model.TrendDirection;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks performance history and detects trends for each student.
 *
 * Stores the previous average score for each student (keyed by ID).
 * When a new average is recorded, compares it against the stored
 * value to classify the trend as IMPROVING, STABLE, or DECLINING.
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

    public TrendAnalyzer() {
        this.previousAverages = new HashMap<>();
        this.currentTrends = new HashMap<>();
    }

    /**
     * Records a new average score for the student and updates the trend.
     *
     * Call this AFTER the student's subjects have been updated so that
     * Student.getAverageScore() returns the new value.
     *
     * @param studentId    the student's unique ID
     * @param newAverage   the student's current average score
     */
    public synchronized void recordAverage(String studentId, double newAverage) {
        if (studentId == null) return;

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
    }
}
