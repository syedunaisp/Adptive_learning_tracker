package tracker.service;

import tracker.data.dao.GoalDAO;
import tracker.data.dao.InterventionDAO;
import tracker.model.InterventionLog;
import tracker.model.InterventionPlan;
import tracker.model.LearningProfile;
import tracker.model.RiskScore;

import java.util.List;

/**
 * Service for managing the intervention lifecycle.
 *
 * Orchestrates:
 * 1. Creating intervention plans
 * 2. Logging intervention activities
 * 3. Status transitions (ACTIVE → COMPLETED/CANCELLED)
 * 4. Rule-based intervention suggestions
 */
public class InterventionEngine {

    private final InterventionDAO interventionDAO;
    private final GoalDAO goalDAO;

    public InterventionEngine(InterventionDAO interventionDAO, GoalDAO goalDAO) {
        this.interventionDAO = interventionDAO;
        this.goalDAO = goalDAO;
    }

    /**
     * Creates a new intervention plan.
     *
     * @return the created plan's ID, or -1 on failure
     */
    public int createPlan(int studentDbId, int teacherDbId, String type, String description) {
        if (type == null || type.trim().isEmpty()) {
            System.err.println("InterventionEngine: Intervention type is required.");
            return -1;
        }
        return interventionDAO.insertPlan(studentDbId, teacherDbId, type.trim(), description);
    }

    /**
     * Marks an intervention plan as COMPLETED.
     */
    public boolean completePlan(int planId) {
        return interventionDAO.updatePlanStatus(planId, "COMPLETED");
    }

    /**
     * Marks an intervention plan as CANCELLED.
     */
    public boolean cancelPlan(int planId) {
        return interventionDAO.updatePlanStatus(planId, "CANCELLED");
    }

    /**
     * Logs an activity against an intervention plan.
     */
    public boolean logActivity(int planId, String notes, String outcomeMetric) {
        return interventionDAO.insertLog(planId, notes, outcomeMetric);
    }

    /**
     * Returns all intervention plans for a student.
     */
    public List<InterventionPlan> getStudentPlans(int studentDbId) {
        return interventionDAO.getPlansByStudent(studentDbId);
    }

    /**
     * Returns all intervention plans created by a teacher.
     */
    public List<InterventionPlan> getTeacherPlans(int teacherDbId) {
        return interventionDAO.getPlansByTeacher(teacherDbId);
    }

    /**
     * Returns all activity logs for a plan.
     */
    public List<InterventionLog> getPlanLogs(int planId) {
        return interventionDAO.getLogsByPlan(planId);
    }

    /**
     * Suggests an intervention type based on risk level and learning profile.
     *
     * Rule-based logic:
     * HIGH risk + Memorization/Visual → TUTORING
     * HIGH risk + Practice-Based/Conceptual → PRACTICE
     * HIGH risk + Mixed/null → TUTORING (default intensive)
     * MODERATE risk → TOPIC_REVIEW
     * LOW risk → STRATEGY_ADJUSTMENT
     *
     * @param risk    the student's current risk score
     * @param profile the student's learning profile (may be null)
     * @return a suggestion string in format "TYPE: Description"
     */
    public String suggestIntervention(RiskScore risk, LearningProfile profile) {
        if (risk == null) {
            return "STRATEGY_ADJUSTMENT: Complete a risk assessment first.";
        }

        RiskScore.Level level = risk.getLevel();
        String profileType = (profile != null) ? profile.getProfileName() : "Unknown";

        switch (level) {
            case HIGH:
                switch (profileType) {
                    case "Memorization-Heavy":
                        return "TUTORING: Intensive revision sessions with spaced repetition drills. " +
                                "Focus on rote memorization of key formulas and facts.";
                    case "Visual":
                        return "TUTORING: Visual-heavy tutoring with diagrams, mind maps, and " +
                                "annotated charts. Use video explanations for complex topics.";
                    case "Practice-Based":
                        return "PRACTICE: Daily problem-solving drills with escalating difficulty. " +
                                "Start with foundational problems and build up.";
                    case "Conceptual":
                        return "PRACTICE: Concept-deep practice with Feynman technique sessions. " +
                                "Focus on understanding 'why' before 'how'.";
                    default:
                        return "TUTORING: Intensive one-on-one tutoring sessions targeting " +
                                "weakest subjects with mixed learning strategies.";
                }
            case MODERATE:
                return "TOPIC_REVIEW: Focus on weakest topics identified in gap analysis. " +
                        "Schedule targeted review sessions for subjects below 70%.";
            case LOW:
            default:
                return "STRATEGY_ADJUSTMENT: Optimize current study habits for advanced performance. " +
                        "Consider acceleration or enrichment activities.";
        }
    }
}
