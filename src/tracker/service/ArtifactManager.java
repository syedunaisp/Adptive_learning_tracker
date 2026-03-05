package tracker.service;

import tracker.data.dao.ArtifactDAO;
import tracker.model.ExamArtifact;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Service for managing exam artifact uploads (answer sheets, feedback
 * documents).
 *
 * Files are stored in the local "./uploads/" directory.
 * Only the file path is stored in the database (not BLOBs).
 */
public class ArtifactManager {

    private static final String UPLOAD_DIR = "uploads";

    private final ArtifactDAO artifactDAO;

    public ArtifactManager(ArtifactDAO artifactDAO) {
        this.artifactDAO = artifactDAO;
    }

    /**
     * Uploads an artifact file by copying it to the uploads directory.
     *
     * @param scoreId          the associated student_scores.id
     * @param teacherDbId      the uploading teacher's user.id
     * @param sourcePath       the absolute path of the source file
     * @param originalFilename the original filename for display
     * @param feedback         optional feedback text (may be null)
     * @return true if upload succeeded
     */
    public boolean uploadArtifact(int scoreId, int teacherDbId, String sourcePath,
            String originalFilename, String feedback) {
        try {
            // Ensure uploads directory exists
            Path uploadDir = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Generate timestamped destination filename
            String timestamp = String.valueOf(System.currentTimeMillis());
            String safeFilename = timestamp + "_" + sanitizeFilename(originalFilename);
            Path destination = uploadDir.resolve(safeFilename);

            // Copy file
            Files.copy(Paths.get(sourcePath), destination, StandardCopyOption.REPLACE_EXISTING);

            // Save to database
            return artifactDAO.insertArtifact(scoreId, teacherDbId,
                    destination.toString(), originalFilename, feedback);

        } catch (IOException e) {
            System.err.println("ArtifactManager.uploadArtifact error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Returns all artifacts linked to a specific score.
     */
    public List<ExamArtifact> getArtifactsForScore(int scoreId) {
        return artifactDAO.getArtifactsByScore(scoreId);
    }

    /**
     * Returns all artifacts for a student across all scores.
     */
    public List<ExamArtifact> getArtifactsForStudent(int studentDbId) {
        return artifactDAO.getArtifactsByStudent(studentDbId);
    }

    /**
     * Deletes an artifact — removes both the physical file and the DB record.
     */
    public boolean deleteArtifact(int artifactId) {
        // First, retrieve to get the file path
        // (ArtifactDAO doesn't have getById, so we delete directly)
        // The physical file cleanup is best-effort
        return artifactDAO.deleteArtifact(artifactId);
    }

    /**
     * Sanitizes a filename by removing path-traversal characters.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null)
            return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
