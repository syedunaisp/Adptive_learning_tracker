package tracker.model;

/**
 * Represents a topic-level score within a subject score.
 */
public class TopicScore {
    private int id;
    private int scoreId;
    private int topicId;
    private double scoreValue;
    private String proficiencyLevel; // WEAK, MODERATE, STRONG, UNKNOWN

    public TopicScore() {
    }

    public TopicScore(int id, int scoreId, int topicId, double scoreValue, String proficiencyLevel) {
        this.id = id;
        this.scoreId = scoreId;
        this.topicId = topicId;
        this.scoreValue = scoreValue;
        this.proficiencyLevel = proficiencyLevel;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScoreId() {
        return scoreId;
    }

    public void setScoreId(int scoreId) {
        this.scoreId = scoreId;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public double getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(double scoreValue) {
        this.scoreValue = scoreValue;
    }

    public String getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(String proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }

    @Override
    public String toString() {
        return String.format("TopicScore[topic=%d, score=%.1f, level=%s]", topicId, scoreValue, proficiencyLevel);
    }
}
