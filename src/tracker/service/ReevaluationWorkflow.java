package tracker.service;

import tracker.data.dao.ReevaluationDAO;
import tracker.model.ReevaluationRequest;

import java.util.List;

/**
 * Service for managing the re-evaluation request workflow.
 *
 * Flow: Student submits → Teacher reviews → Approve or Reject
 */
public class ReevaluationWorkflow {

    private static final int MIN_REASON_LENGTH = 10;

    private final ReevaluationDAO reevaluationDAO;

    public ReevaluationWorkflow(ReevaluationDAO reevaluationDAO) {
        this.reevaluationDAO = reevaluationDAO;
    }

    /**
     * Submits a re-evaluation request from a student.
     *
     * Validation: reason must be at least 10 characters.
     *
     * @param scoreId     the student_scores.id being contested
     * @param studentDbId the student's DB primary key
     * @param reason      the reason for requesting re-evaluation
     * @return true if submitted successfully
     */
    public boolean submitRequest(int scoreId, int studentDbId, String reason) {
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            System.err.println("ReevaluationWorkflow: Reason must be at least "
                    + MIN_REASON_LENGTH + " characters.");
            return false;
        }
        return reevaluationDAO.submitRequest(scoreId, studentDbId, reason.trim());
    }

    /**
     * Returns all requests submitted by a student.
     */
    public List<ReevaluationRequest> getStudentRequests(int studentDbId) {
        return reevaluationDAO.getRequestsByStudent(studentDbId);
    }

    /**
     * Returns all pending requests (for teacher review).
     */
    public List<ReevaluationRequest> getPendingRequests() {
        return reevaluationDAO.getPendingRequests();
    }

    /**
     * Approves a re-evaluation request.
     */
    public boolean approveRequest(int requestId, int teacherUserId, String notes) {
        return reevaluationDAO.resolveRequest(requestId, teacherUserId,
                notes != null ? notes : "Approved");
    }

    /**
     * Rejects a re-evaluation request.
     */
    public boolean rejectRequest(int requestId, int teacherUserId, String notes) {
        return reevaluationDAO.rejectRequest(requestId, teacherUserId,
                notes != null ? notes : "Rejected");
    }
}
