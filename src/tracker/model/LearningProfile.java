package tracker.model;

/**
 * Represents a learning profile type (e.g., Visual, Conceptual).
 */
public class LearningProfile {
    private int id;
    private String profileName;
    private String description;

    public LearningProfile() {
    }

    public LearningProfile(int id, String profileName, String description) {
        this.id = id;
        this.profileName = profileName;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return profileName;
    }
}
