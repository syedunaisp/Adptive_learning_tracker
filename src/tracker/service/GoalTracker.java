package tracker.service;

import tracker.data.dao.GoalDAO;
import tracker.model.Goal;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing student academic goals.
 *
 * Orchestrates:
 * 1. Setting goals (overall or per-subject)
 * 2. Checking progress against targets
 * 3. Auto-marking goals as ACHIEVED when met
 * 4. Abandoning goals
 */
public class GoalTracker {

    private final GoalDAO goalDAO;

    public GoalTracker(GoalDAO goalDAO) {
        this.goalDAO = goalDAO;
    }

    /**
     * Sets a new academic goal for a student.
     *
     * @param studentDbId the student's DB primary key
     * @param subjectDbId the subject (null for overall average goal)
     * @param targetScore the target score to achieve
     * @param deadline    optional deadline (ISO date string, may be null)
     * @return true if the goal was saved
     */
    public boolean setGoal(int studentDbId, Integer subjectDbId, double targetScore, String deadline) {
        if (targetScore < 0 || targetScore > 100) {
            System.err.println("GoalTracker: Target score must be between 0 and 100.");
            return false;
        }
        return goalDAO.insertGoal(studentDbId, subjectDbId, targetScore, deadline);
    }

    /**
     * Returns only ACTIVE goals for a student.
     */
    public List<Goal> getActiveGoals(int studentDbId) {
        List<Goal> all = goalDAO.getGoalsByStudent(studentDbId);
        List<Goal> active = new ArrayList<>();
        for (Goal g : all) {
            if ("ACTIVE".equals(g.getStatus())) {
                active.add(g);
            }
        }
        return active;
    }

    /**
     * Returns all goals for a student (all statuses).
     */
    public List<Goal> getAllGoals(int studentDbId) {
        return goalDAO.getGoalsByStudent(studentDbId);
    }

    /**
     * Checks progress of all ACTIVE goals against current performance.
     * Automatically marks goals as ACHIEVED if the target is met.
     *
     * @param studentDbId    the student's DB primary key
     * @param currentAverage the student's current overall average
     * @param subjectScores  map of subjectDbId → current score (for per-subject
     *                       goals)
     * @return list of progress status messages
     */
    public List<String> checkGoalProgress(int studentDbId, double currentAverage,
            java.util.Map<Integer, Double> subjectScores) {
        List<String> messages = new ArrayList<>();
        List<Goal> activeGoals = getActiveGoals(studentDbId);

        for (Goal goal : activeGoals) {
            double current;
            String label;

            if (goal.getSubjectId() == null) {
                // Overall average goal
                current = currentAverage;
                label = "Overall Average";
            } else {
                // Per-subject goal
                Double subjectScore = (subjectScores != null)
                        ? subjectScores.get(goal.getSubjectId())
                        : null;
                if (subjectScore == null) {
                    messages.add(String.format("Goal (ID: %d): No score data available yet.", goal.getId()));
                    continue;
                }
                current = subjectScore;
                label = "Subject #" + goal.getSubjectId();
            }

            double target = goal.getTargetScore();
            double progress = (target > 0) ? (current / target) * 100.0 : 100.0;

            if (current >= target) {
                // Auto-achieve
                goalDAO.updateGoalStatus(goal.getId(), "ACHIEVED");
                messages.add(String.format("✓ ACHIEVED: %s goal reached! %.1f/%.1f (100%%)",
                        label, current, target));
            } else {
                messages.add(String.format("→ %s goal: %.1f/%.1f (%.0f%% progress)",
                        label, current, target, progress));
            }
        }

        if (activeGoals.isEmpty()) {
            messages.add("No active goals set. Use goal tracking to set academic targets.");
        }

        return messages;
    }

    /**
     * Abandons a goal by setting its status to ABANDONED.
     */
    public boolean abandonGoal(int goalId) {
        return goalDAO.updateGoalStatus(goalId, "ABANDONED");
    }
}
