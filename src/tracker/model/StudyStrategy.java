package tracker.model;

/**
 * Represents a study strategy linked to a learning profile.
 */
public class StudyStrategy {
    private int id;
    private int profileId;
    private String strategyName;
    private String instructions;

    public StudyStrategy() {
    }

    public StudyStrategy(int id, int profileId, String strategyName, String instructions) {
        this.id = id;
        this.profileId = profileId;
        this.strategyName = strategyName;
        this.instructions = instructions;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProfileId() {
        return profileId;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    @Override
    public String toString() {
        return strategyName;
    }
}
