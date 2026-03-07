import tracker.data.DataManager;
import tracker.model.Student;
import tracker.model.RiskScore;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;

public class TestMath {
    public static void main(String[] args) throws Exception {
        DataManager dm = new DataManager();
        TrendAnalyzer ta = new TrendAnalyzer();
        RiskPredictor rp = new RiskPredictor(ta);

        for (Student s : dm.getStudents()) {
            if (!s.getSubjects().isEmpty()) {
                ta.recordAverage(s.getId(), s.getAverageScore());
            }
        }

        for (Student s : dm.getStudents()) {
            RiskScore score = rp.assessRisk(s);
            System.out.println("Student: " + s.getName() + " | Avg: " + s.getAverageScore() + " | NumericRisk: "
                    + score.getNumericScore() + " | Level: " + score.getLevel().getLabel());
            for (String ex : score.getExplanations()) {
                System.out.println("   " + ex);
            }
        }
    }
}
