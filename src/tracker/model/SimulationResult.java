package tracker.model;

import java.util.List;

/**
 * Holds the result of a "what-if" risk simulation.
 * Contains the modified scores, recalculated average, new risk assessment,
 * and updated recommendations -- without modifying the original student data.
 */
public class SimulationResult {

    private final String studentId;
    private final String studentName;
    private final String scenarioDescription;

    private final double originalAverage;
    private final double simulatedAverage;

    private final RiskScore originalRisk;
    private final RiskScore simulatedRisk;

    private final List<String> simulatedRecommendations;

    public SimulationResult(String studentId, String studentName,
                            String scenarioDescription,
                            double originalAverage, double simulatedAverage,
                            RiskScore originalRisk, RiskScore simulatedRisk,
                            List<String> simulatedRecommendations) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.scenarioDescription = scenarioDescription;
        this.originalAverage = originalAverage;
        this.simulatedAverage = simulatedAverage;
        this.originalRisk = originalRisk;
        this.simulatedRisk = simulatedRisk;
        this.simulatedRecommendations = simulatedRecommendations;
    }

    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getScenarioDescription() { return scenarioDescription; }
    public double getOriginalAverage() { return originalAverage; }
    public double getSimulatedAverage() { return simulatedAverage; }
    public RiskScore getOriginalRisk() { return originalRisk; }
    public RiskScore getSimulatedRisk() { return simulatedRisk; }
    public List<String> getSimulatedRecommendations() { return simulatedRecommendations; }

    public double getAverageDelta() {
        return simulatedAverage - originalAverage;
    }

    public double getRiskDelta() {
        return simulatedRisk.getNumericScore() - originalRisk.getNumericScore();
    }

    /**
     * Returns a formatted report of the simulation outcome.
     */
    public String getReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================\n");
        sb.append("     RISK SIMULATION REPORT\n");
        sb.append("============================================\n");
        sb.append("Student    : ").append(studentName).append(" (").append(studentId).append(")\n");
        sb.append("Scenario   : ").append(scenarioDescription).append("\n\n");

        sb.append("--- Average Score ---\n");
        sb.append(String.format("  Before: %.2f\n", originalAverage));
        sb.append(String.format("  After : %.2f  (%+.2f)\n\n", simulatedAverage, getAverageDelta()));

        sb.append("--- Risk Level ---\n");
        sb.append(String.format("  Before: %s (%.1f)\n",
                originalRisk.getLevel().getLabel(), originalRisk.getNumericScore()));
        sb.append(String.format("  After : %s (%.1f)  (%+.1f)\n\n",
                simulatedRisk.getLevel().getLabel(), simulatedRisk.getNumericScore(), getRiskDelta()));

        sb.append("--- Simulated Recommendations ---\n");
        if (simulatedRecommendations == null || simulatedRecommendations.isEmpty()) {
            sb.append("  No specific recommendations.\n");
        } else {
            for (String rec : simulatedRecommendations) {
                sb.append("  ").append(rec).append("\n");
            }
        }
        sb.append("============================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("Simulation[%s]: avg %.2f->%.2f, risk %s->%s",
                scenarioDescription, originalAverage, simulatedAverage,
                originalRisk.getLevel(), simulatedRisk.getLevel());
    }
}
