package tracker.service.ai;

import tracker.model.RiskScore;
import tracker.model.Student;
import tracker.model.Subject;
import tracker.model.SubjectCategory;
import tracker.model.TrendDirection;

import java.util.*;

/**
 * Adaptive recommendation planner that generates dynamic, context-aware
 * study recommendations based on multiple factors:
 *
 *   1. Risk severity level
 *   2. Individual subject weaknesses and their scores
 *   3. Category-level weakness patterns (e.g., weak across all STEM)
 *   4. Performance trend direction
 *
 * This service delegates subject-specific recommendations to
 * {@link tracker.service.RecommendationEngine} and layers additional
 * strategic recommendations on top.
 *
 * No hardcoded UI logic -- all outputs are plain data structures
 * consumable by any presentation layer.
 */
public class AdaptivePlanner {

    /**
     * Generates a comprehensive, prioritized list of recommendations.
     *
     * @param student the student to generate recommendations for
     * @param risk    the student's computed risk score
     * @param trend   the student's performance trend
     * @return ordered list of recommendation strings
     */
    public List<String> generateRecommendations(Student student,
                                                 RiskScore risk,
                                                 TrendDirection trend) {
        List<String> recommendations = new ArrayList<>();

        if (student == null || student.getSubjects() == null
                || student.getSubjects().isEmpty()) {
            recommendations.add("No subject data available. Please add subjects to receive recommendations.");
            return recommendations;
        }

        List<Subject> weakSubjects = student.getWeakSubjects();

        // --- 1. Severity-based urgency header ---
        addSeverityRecommendations(recommendations, risk, weakSubjects);

        // --- 2. Category-level weakness detection ---
        addCategoryRecommendations(recommendations, weakSubjects, student.getSubjects());

        // --- 3. Individual subject recommendations ---
        addSubjectRecommendations(recommendations, weakSubjects);

        // --- 4. Trend-based strategic advice ---
        addTrendRecommendations(recommendations, trend, risk);

        // --- 5. Positive reinforcement if applicable ---
        if (weakSubjects.isEmpty()) {
            recommendations.add("[STRENGTH] All subjects are above the 60-point threshold.");
            recommendations.add("   -> Continue current study habits and aim for excellence.");
            recommendations.add("   -> Consider mentoring peers who may need support.");
        }

        return recommendations;
    }

    /**
     * Adds urgency-level recommendations based on risk severity.
     */
    private void addSeverityRecommendations(List<String> recs,
                                             RiskScore risk,
                                             List<Subject> weakSubjects) {
        switch (risk.getLevel()) {
            case HIGH:
                recs.add("[CRITICAL] High Risk Detected (Score: "
                        + String.format("%.1f", risk.getNumericScore()) + "/100)");
                recs.add("   -> IMMEDIATE intervention recommended.");
                recs.add("   -> Schedule meeting with academic advisor within this week.");
                recs.add("   -> Prioritize the weakest subjects listed below.");
                if (weakSubjects.size() >= 3) {
                    recs.add("   -> Multiple subject failures detected -- consider a reduced course load.");
                }
                break;
            case MODERATE:
                recs.add("[WARNING] Moderate Risk Detected (Score: "
                        + String.format("%.1f", risk.getNumericScore()) + "/100)");
                recs.add("   -> Proactive measures recommended to prevent further decline.");
                recs.add("   -> Create a structured weekly study plan.");
                break;
            case LOW:
                recs.add("[INFO] Low Risk (Score: "
                        + String.format("%.1f", risk.getNumericScore()) + "/100)");
                recs.add("   -> Student is performing well overall.");
                break;
        }
    }

    /**
     * Detects category-level weaknesses (e.g., weak in all STEM subjects)
     * and adds strategic recommendations.
     */
    private void addCategoryRecommendations(List<String> recs,
                                             List<Subject> weakSubjects,
                                             List<Subject> allSubjects) {
        if (weakSubjects.isEmpty()) return;

        // Count weak subjects per category
        Map<SubjectCategory, Integer> weakByCategory = new EnumMap<>(SubjectCategory.class);
        Map<SubjectCategory, Integer> totalByCategory = new EnumMap<>(SubjectCategory.class);

        for (Subject s : allSubjects) {
            SubjectCategory cat = SubjectCategory.categorize(s.getSubjectName());
            totalByCategory.merge(cat, 1, Integer::sum);
        }

        for (Subject s : weakSubjects) {
            SubjectCategory cat = SubjectCategory.categorize(s.getSubjectName());
            weakByCategory.merge(cat, 1, Integer::sum);
        }

        // If all subjects in a category are weak, flag it
        for (Map.Entry<SubjectCategory, Integer> entry : weakByCategory.entrySet()) {
            SubjectCategory cat = entry.getKey();
            if (cat == SubjectCategory.UNCATEGORIZED) continue;

            int weakCount = entry.getValue();
            Integer total = totalByCategory.get(cat);
            if (total == null) continue;

            if (weakCount >= 2 && weakCount == total) {
                recs.add("[CATEGORY ALERT] Weakness across entire " + cat.getDisplayName() + " category");
                recs.add("   -> This suggests foundational gaps in " + cat.getDisplayName() + ".");
                recs.add("   -> Consider enrolling in a bridging/remedial program for " + cat.getDisplayName() + ".");
            } else if (weakCount >= 2) {
                recs.add("[CATEGORY WARNING] Multiple weak subjects in " + cat.getDisplayName()
                        + " (" + weakCount + "/" + total + ")");
                recs.add("   -> Focus study sessions on " + cat.getDisplayName() + " fundamentals.");
            }
        }
    }

    /**
     * Adds per-subject recommendations for each weak subject.
     */
    private void addSubjectRecommendations(List<String> recs,
                                            List<Subject> weakSubjects) {
        if (weakSubjects.isEmpty()) return;

        // Sort by score ascending (worst first)
        List<Subject> sorted = new ArrayList<>(weakSubjects);
        sorted.sort(Comparator.comparingDouble(Subject::getScore));

        recs.add("");
        recs.add("--- Subject-Specific Recommendations (ordered by severity) ---");

        for (Subject s : sorted) {
            double score = s.getScore();
            String name = s.getSubjectName();
            SubjectCategory cat = SubjectCategory.categorize(name);

            recs.add(String.format("[%s] Score: %.1f | Category: %s",
                    name, score, cat.getDisplayName()));

            if (score < 30) {
                recs.add("   -> CRITICAL: Score dangerously low. Requires intensive tutoring.");
                recs.add("   -> Review all foundational concepts from scratch.");
                recs.add("   -> Attend every available help session and office hour.");
            } else if (score < 45) {
                recs.add("   -> Significant weakness. Dedicate extra study hours each week.");
                recs.add("   -> Practice past exam papers to identify knowledge gaps.");
                recs.add("   -> Form a study group focused on this subject.");
            } else {
                recs.add("   -> Close to passing threshold. Targeted revision should help.");
                recs.add("   -> Focus on weak chapters and practice problem sets.");
                recs.add("   -> Use online resources and video tutorials for reinforcement.");
            }
        }
    }

    /**
     * Adds trend-based strategic recommendations.
     */
    private void addTrendRecommendations(List<String> recs,
                                          TrendDirection trend,
                                          RiskScore risk) {
        recs.add("");
        recs.add("--- Trend-Based Advice ---");

        switch (trend) {
            case DECLINING:
                recs.add("[TREND] Performance is DECLINING.");
                recs.add("   -> Identify recent changes (attendance, study habits, personal issues).");
                recs.add("   -> Consider meeting with a counselor or mentor.");
                if (risk.getLevel() == RiskScore.Level.HIGH) {
                    recs.add("   -> URGENT: Declining trend combined with high risk demands immediate action.");
                }
                break;
            case STABLE:
                recs.add("[TREND] Performance is STABLE.");
                if (risk.getLevel() == RiskScore.Level.HIGH || risk.getLevel() == RiskScore.Level.MODERATE) {
                    recs.add("   -> Stable but at risk -- current efforts are not sufficient.");
                    recs.add("   -> Intensify study habits and seek additional support.");
                } else {
                    recs.add("   -> Maintain current study strategies.");
                    recs.add("   -> Set goals to push for improvement in the next assessment.");
                }
                break;
            case IMPROVING:
                recs.add("[TREND] Performance is IMPROVING.");
                recs.add("   -> Positive momentum detected! Continue current strategies.");
                recs.add("   -> Build on this progress by setting stretch goals.");
                break;
        }
    }
}
