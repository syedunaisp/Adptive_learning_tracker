package tracker.model;

/**
 * Represents an uploaded exam artifact (answer sheet, feedback document).
 */
public class ExamArtifact {
    private int id;
    private int scoreId;
    private int teacherId;
    private String filePath;
    private String originalFilename;
    private String uploadDate;
    private String feedbackText;

    public ExamArtifact() {
    }

    public ExamArtifact(int id, int scoreId, int teacherId, String filePath,
            String originalFilename, String uploadDate, String feedbackText) {
        this.id = id;
        this.scoreId = scoreId;
        this.teacherId = teacherId;
        this.filePath = filePath;
        this.originalFilename = originalFilename;
        this.uploadDate = uploadDate;
        this.feedbackText = feedbackText;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getScoreId() {
        return scoreId;
    }

    public void setScoreId(int scoreId) {
        this.scoreId = scoreId;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    @Override
    public String toString() {
        return String.format("Artifact[%s, uploaded=%s]", originalFilename, uploadDate);
    }
}
