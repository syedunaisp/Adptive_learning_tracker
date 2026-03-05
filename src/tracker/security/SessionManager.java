package tracker.security;

import tracker.model.User;
import tracker.model.UserRole;

/**
 * Manages the current user session at runtime.
 *
 * Stores:
 *   - The authenticated User object
 *   - Role
 *   - Linked student_id (if role is STUDENT)
 *   - Permissions derived from role
 *
 * Accessible globally via static methods.
 * Thread-safe for Swing single-EDT usage.
 */
public final class SessionManager {

    private static User currentUser;
    private static String linkedStudentId;  // business student_id (e.g., "S001")

    /**
     * Starts a new session for the authenticated user.
     */
    public static void login(User user, String studentId) {
        currentUser = user;
        linkedStudentId = studentId;
    }

    /**
     * Ends the current session.
     */
    public static void logout() {
        currentUser = null;
        linkedStudentId = null;
    }

    /**
     * Returns the currently logged-in user, or null if no session.
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Returns the current user's role, or null if no session.
     */
    public static UserRole getRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    /**
     * Returns the username of the current user, or "Guest".
     */
    public static String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "Guest";
    }

    /**
     * Returns the business student_id linked to this user (for STUDENT role).
     * Returns null for non-student roles or if not linked.
     */
    public static String getLinkedStudentId() {
        return linkedStudentId;
    }

    /**
     * Returns true if a user is currently logged in.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * Permission: can the current user edit student data?
     */
    public static boolean canEditData() {
        return currentUser != null && currentUser.getRole().canEditData();
    }

    /**
     * Permission: can the current user access admin features?
     */
    public static boolean canAccessAdmin() {
        return currentUser != null && currentUser.getRole().canAccessAdmin();
    }

    /**
     * Permission: can the current user access analytics?
     */
    public static boolean canAccessAnalytics() {
        return currentUser != null && currentUser.getRole().canAccessAnalytics();
    }

    /**
     * Permission: can the current user access simulation?
     */
    public static boolean canAccessSimulation() {
        return currentUser != null && currentUser.getRole().canAccessSimulation();
    }

    /**
     * Permission: can the current user export reports?
     */
    public static boolean canExportReports() {
        return currentUser != null && currentUser.getRole().canExportReports();
    }

    private SessionManager() { }
}
