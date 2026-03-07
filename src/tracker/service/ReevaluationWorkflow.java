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
     * @param subjectName the subject being contested
     * @param studentDbId the student's DB primary key
     * @param reason      the reason for requesting re-evaluation
     * @param teacherId   the teacher user ID to route the request to
     * @return true if submitted successfully
     */
    public boolean submitRequest(String subjectName, int studentDbId, String reason, int teacherId) {
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            System.err.println("ReevaluationWorkflow: Reason must be at least "
                    + MIN_REASON_LENGTH + " characters.");
            return false;
        }
        if (subjectName == null || subjectName.trim().isEmpty()) {
            System.err.println("ReevaluationWorkflow: Subject name is required.");
            return false;
        }
        return reevaluationDAO.submitRequest(subjectName.trim(), studentDbId, reason.trim(), teacherId);
    }

    /**
     * Returns all requests submitted by a student.
     */
    public List<ReevaluationRequest> getStudentRequests(int studentDbId) {
        return reevaluationDAO.getRequestsByStudent(studentDbId);
    }

    /**
     * Returns all pending requests for a specific teacher.
     */
    public List<ReevaluationRequest> getPendingRequestsForTeacher(int teacherUserId) {
        return reevaluationDAO.getPendingRequestsForTeacher(teacherUserId);
    }

    /**
     * Approves a re-evaluation request with updated marks and reasoning.
     */
    public boolean approveRequest(int requestId, int teacherUserId, String notes, double updatedMarks) {
        return reevaluationDAO.resolveRequest(requestId, teacherUserId,
                notes != null ? notes : "Approved", updatedMarks);
    }

    /**
     * Rejects a re-evaluation request with reasoning.
     */
    public boolean rejectRequest(int requestId, int teacherUserId, String notes) {
        return reevaluationDAO.rejectRequest(requestId, teacherUserId,
                notes != null ? notes : "Rejected");
    }

    /**
     * Looks up the teacher user ID for a given student.
     */
    public int getStudentTeacherId(int studentDbId) {
        return reevaluationDAO.getStudentTeacherId(studentDbId);
    }

    /**
     * Gets the student name by their DB id.
     */
    public String getStudentNameById(int studentDbId) {
        return reevaluationDAO.getStudentNameById(studentDbId);
    }
}
