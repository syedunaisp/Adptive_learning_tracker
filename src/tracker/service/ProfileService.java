package tracker.service;

import tracker.data.dao.ProfileDAO;
import tracker.model.LearningProfile;
import tracker.model.StudyStrategy;

import java.util.List;
import java.util.Map;

/**
 * Service for managing the learning profile questionnaire workflow.
 *
 * Orchestrates:
 * 1. Presenting questionnaire questions
 * 2. Processing answers to determine learning profile
 * 3. Saving the profile assignment
 * 4. Retrieving strategies for the assigned profile
 */
public class ProfileService {

    private final ProfileDAO profileDAO;

    public ProfileService(ProfileDAO profileDAO) {
        this.profileDAO = profileDAO;
    }

    /**
     * Determines and saves a learning profile based on questionnaire responses.
     *
     * Logic: Count answer frequencies:
     * Mostly A → Memorization-Heavy (id=1)
     * Mostly B → Conceptual (id=2)
     * Mostly C → Practice-Based (id=3)
     * Mostly D → Visual (id=4)
     * Tie → Mixed (id=5)
     *
     * @param studentDbId the student's DB primary key
     * @param answers     map of questionId → answer value (A, B, C, or D)
     * @return the assigned LearningProfile
     */
    public LearningProfile determineProfile(int studentDbId, Map<Integer, String> answers) {
        if (answers == null || answers.isEmpty()) {
            return null;
        }

        // Save individual responses
        for (Map.Entry<Integer, String> entry : answers.entrySet()) {
            profileDAO.saveQuestionnaireResponse(studentDbId, entry.getKey(), entry.getValue());
        }

        // Count answer frequencies
        int countA = 0, countB = 0, countC = 0, countD = 0;
        for (String answer : answers.values()) {
            if (answer == null)
                continue;
            switch (answer.toUpperCase().trim()) {
                case "A":
                    countA++;
                    break;
                case "B":
                    countB++;
                    break;
                case "C":
                    countC++;
                    break;
                case "D":
                    countD++;
                    break;
            }
        }

        // Determine profile ID based on majority answer
        int profileId;
        int max = Math.max(Math.max(countA, countB), Math.max(countC, countD));

        if (max == 0) {
            profileId = 5; // Mixed
        } else if (countA == max && countB != max && countC != max && countD != max) {
            profileId = 1; // Memorization-Heavy
        } else if (countB == max && countA != max && countC != max && countD != max) {
            profileId = 2; // Conceptual
        } else if (countC == max && countA != max && countB != max && countD != max) {
            profileId = 3; // Practice-Based
        } else if (countD == max && countA != max && countB != max && countC != max) {
            profileId = 4; // Visual
        } else {
            profileId = 5; // Tie → Mixed
        }

        // Save profile assignment
        profileDAO.saveStudentProfile(studentDbId, profileId);

        return profileDAO.getStudentProfile(studentDbId);
    }

    /**
     * Returns the current learning profile for a student, or null if not assigned.
     */
    public LearningProfile getStudentProfile(int studentDbId) {
        return profileDAO.getStudentProfile(studentDbId);
    }

    /**
     * Returns the study strategies for a student's assigned profile.
     */
    public List<StudyStrategy> getStudentStrategies(int studentDbId) {
        LearningProfile profile = profileDAO.getStudentProfile(studentDbId);
        if (profile == null) {
            return List.of();
        }
        return profileDAO.getStrategiesByProfile(profile.getId());
    }

    /**
     * Returns all available learning profiles.
     */
    public List<LearningProfile> getAllProfiles() {
        return profileDAO.getAllProfiles();
    }

    /**
     * Returns all questionnaire questions.
     */
    public List<String[]> getQuestionnaireQuestions() {
        return profileDAO.getQuestionnaireQuestions();
    }
}
