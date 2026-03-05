package tracker.model;

/**
 * Represents a platform user with authentication and role data.
 * Maps to the 'users' table in the database.
 */
public class User {

    private int id;
    private String username;
    private String passwordHash;
    private UserRole role;
    private Integer linkedStudentDbId;  // DB primary key of linked student (nullable)
    private String linkedStudentId;     // Business student_id (e.g., "S001") — transient
    private boolean enabled;
    private String createdAt;

    public User() { }

    public User(String username, String passwordHash, UserRole role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.enabled = true;
    }

    // --- Getters ---

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public Integer getLinkedStudentDbId() { return linkedStudentDbId; }
    public String getLinkedStudentId() { return linkedStudentId; }
    public boolean isEnabled() { return enabled; }
    public String getCreatedAt() { return createdAt; }

    // --- Setters ---

    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(UserRole role) { this.role = role; }
    public void setLinkedStudentDbId(Integer linkedStudentDbId) { this.linkedStudentDbId = linkedStudentDbId; }
    public void setLinkedStudentId(String linkedStudentId) { this.linkedStudentId = linkedStudentId; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + ", enabled=" + enabled + "}";
    }
}
