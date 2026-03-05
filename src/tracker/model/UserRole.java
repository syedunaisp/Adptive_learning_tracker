package tracker.model;

/**
 * Defines the roles available in the ALIP platform.
 * Controls access to features based on role-based restrictions.
 *
 * Student:  View-only access. Cannot edit data. Cannot see Admin.
 * Teacher:  Can add/edit student data. Full analytics/reports. No Admin access.
 * Admin:    Full unrestricted access to all features.
 */
public enum UserRole {

    STUDENT("Student"),
    TEACHER("Teacher"),
    ADMIN("Admin");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Whether this role can add/edit student data. */
    public boolean canEditData() {
        return this != STUDENT;
    }

    /** Whether this role can access the Admin configuration panel. */
    public boolean canAccessAdmin() {
        return this == ADMIN;
    }

    /** Whether this role can run what-if simulations. */
    public boolean canAccessSimulation() {
        return this != STUDENT;
    }

    /** Whether this role can view institutional analytics. */
    public boolean canAccessAnalytics() {
        return this != STUDENT;
    }

    /** Whether this role can export reports. */
    public boolean canExportReports() {
        return this != STUDENT;
    }

    @Override
    public String toString() {
        return label;
    }
}
