package tracker.service.ai;

import tracker.model.RiskScore;
import tracker.model.Student;
import tracker.model.Subject;
import tracker.model.TrendDirection;

import java.util.List;

/**
 * AI-powered risk prediction engine.
 * Replaces the simple threshold-based risk check (average < 50)
 * with a weighted multi-factor scoring model.
 *
 * Inputs:
 *   1. Average score
 *   2. Number of weak subjects (score < 60)
 *   3. Lowest individual score
 *   4. Performance trend direction
 *
 * Output: a {@link RiskScore} containing level, numeric score,
 *         and a detailed explanation breakdown.
 *
 * This class does NOT modify the Student object.
 * It reuses Student.getAverageScore() and Student.getWeakSubjects()
 * to avoid duplicating calculation logic.
 */
public class RiskPredictor {

    private final TrendAnalyzer trendAnalyzer;

    /**
     * Constructs a RiskPredictor with a shared TrendAnalyzer instance.
     *
     * @param trendAnalyzer the trend analyzer to query for trend direction
     */
    public RiskPredictor(TrendAnalyzer trendAnalyzer) {
        this.trendAnalyzer = trendAnalyzer;
    }

    /**
     * Computes a comprehensive risk score for the given student.
     *
     * Reuses:
     *   - student.getAverageScore()   (no duplication)
     *   - student.getWeakSubjects()   (no duplication)
     *
     * @param student the student to evaluate
     * @return a fully computed RiskScore
     */
    public RiskScore assessRisk(Student student) {
        if (student == null || student.getSubjects() == null
                || student.getSubjects().isEmpty()) {
            // No data -- return a safe default
            return new RiskScore(100, 0, 100, 0, TrendDirection.STABLE);
        }

        double average = student.getAverageScore();
        List<Subject> weakSubjects = student.getWeakSubjects();
        int weakCount = weakSubjects.size();
        int totalSubjects = student.getSubjects().size();
        double lowestScore = findLowestScore(student.getSubjects());
        TrendDirection trend = trendAnalyzer.getTrend(student.getId());

        return new RiskScore(average, weakCount, lowestScore,
                totalSubjects, trend);
    }

    /**
     * Computes a risk score using an explicit trend direction override.
     * Used by the simulation engine to test hypothetical scenarios.
     *
     * @param average       the (possibly simulated) average
     * @param weakCount     the (possibly simulated) weak subject count
     * @param lowestScore   the (possibly simulated) lowest score
     * @param totalSubjects total subject count
     * @param trend         the trend direction to use
     * @return a fully computed RiskScore
     */
    public RiskScore assessRisk(double average, int weakCount,
                                double lowestScore, int totalSubjects,
                                TrendDirection trend) {
        return new RiskScore(average, weakCount, lowestScore,
                totalSubjects, trend);
    }

    /**
     * Finds the lowest score across a list of subjects.
     */
    private double findLowestScore(List<Subject> subjects) {
        double lowest = Double.MAX_VALUE;
        for (Subject s : subjects) {
            if (s.getScore() < lowest) {
                lowest = s.getScore();
            }
        }
        return lowest == Double.MAX_VALUE ? 0 : lowest;
    }
}
