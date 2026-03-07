import tracker.data.DataManager;
import tracker.model.Student;
import tracker.model.RiskScore;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;

public class TestMath {
    private static DataManager dataManager = DataManager.getInstance();

    public static void main(String[] args) throws Exception {
        TrendAnalyzer ta = new TrendAnalyzer();
        RiskPredictor rp = new RiskPredictor(ta);

        for (Student s : dataManager.getStudents()) {
            if (!s.getSubjects().isEmpty()) {
                ta.recordAverage(s.getId(), s.getAverageScore());
            }
        }

        for (Student s : dataManager.getStudents()) {
            RiskScore score = rp.assessRisk(s);
            System.out.println("Student: " + s.getName() + " | Avg: " + s.getAverageScore() + " | NumericRisk: "
                    + score.getNumericScore() + " | Level: " + score.getLevel().getLabel());
            for (String ex : score.getExplanations()) {
                System.out.println("   " + ex);
            }
        }
    }
}
