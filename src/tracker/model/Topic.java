package tracker.model;

/**
 * Represents a topic within a subject (e.g., Algebra within Mathematics).
 */
public class Topic {
    private int id;
    private int subjectId;
    private String topicName;

    public Topic() {
    }

    public Topic(int id, int subjectId, String topicName) {
        this.id = id;
        this.subjectId = subjectId;
        this.topicName = topicName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    @Override
    public String toString() {
        return topicName;
    }
}
