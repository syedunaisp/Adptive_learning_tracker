package tracker.service;

import tracker.data.DataManager;
import tracker.data.dao.ClassDAO;
import tracker.model.ClassRoom;
import tracker.model.Student;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing class associations and batch importing students via
 * Excel.
 */
public class StudentManagementService {

    private final DataManager dataManager;
    private final ClassDAO classDAO;

    public StudentManagementService(DataManager dataManager) {
        this.dataManager = dataManager;
        this.classDAO = new ClassDAO();
    }

    public List<ClassRoom> getAllClasses() {
        return classDAO.loadAll();
    }

    public List<ClassRoom> getClassesByTeacher(int teacherId) {
        return classDAO.loadByTeacherId(teacherId);
    }

    public ClassRoom addClass(String className, String section) {
        return addClass(className, section, 0);
    }

    public ClassRoom addClass(String className, String section, int teacherId) {
        int id = classDAO.insert(className, section, teacherId);
        if (id > 0) {
            return new ClassRoom(id, className, section);
        }
        return null; // Failed
    }

    public boolean deleteClass(int id) {
        return classDAO.deleteById(id);
    }

    public List<Student> getStudentsForClass(int classId) {
        return dataManager.getStudents().stream()
                .filter(s -> s.getClassId() == classId)
                .collect(Collectors.toList());
    }

    /**
     * Adds a generic student and attaches to a class.
     */
    public boolean addStudent(String id, String name, int classId, String rollNo, String email) {
        Student newStudent = new Student(id != null && !id.trim().isEmpty() ? id : UUID.randomUUID().toString(), name);
        newStudent.setClassId(classId);
        newStudent.setRollNumber(rollNo);
        newStudent.setEmail(email);

        int primaryKey = dataManager.getStudentDAO().insert(newStudent);
        if (primaryKey > 0) {
            dataManager.refreshCache();
            return true;
        }
        return false;
    }

    /**
     * Imports students from an Excel file into the specified class.
     * Expected columns in 0-indexed order: 0=Name, 1=RollNumber, 2=Email.
     * ID is auto-generated if not provided.
     * 
     * @return Number of successful imports.
     */
    public int importStudentsFromExcel(File file, int targetClassId) {
        int count = 0;
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            boolean isFirstRow = true;

            int nameColIdx = -1, rollColIdx = -1, emailColIdx = -1;
            List<Integer> subjectColIndices = new ArrayList<>();
            List<String> subjectNames = new ArrayList<>();

            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();

            for (Row row : sheet) {
                // Header row: dynamically find columns
                if (isFirstRow) {
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (cell == null)
                            continue;

                        String originalHeader = formatter.formatCellValue(cell).trim();
                        String header = originalHeader.toLowerCase();

                        if (header.contains("name"))
                            nameColIdx = i;
                        else if (header.contains("roll"))
                            rollColIdx = i;
                        else if (header.contains("email"))
                            emailColIdx = i;
                        else if (!header.contains("id") && !header.contains("risk") &&
                                !header.contains("trend") && !header.contains("average") &&
                                !header.contains("action")) {
                            // Any unstructured column is assumed to be a subject
                            subjectColIndices.add(i);
                            subjectNames.add(originalHeader);
                        }
                    }
                    isFirstRow = false;
                    continue;
                }

                if (nameColIdx == -1)
                    break; // Cannot proceed without a name column

                Cell nameCell = row.getCell(nameColIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (nameCell == null)
                    continue;

                String name = formatter.formatCellValue(nameCell);
                if (name == null || name.trim().isEmpty())
                    continue;

                String rollNo = null;
                if (rollColIdx != -1) {
                    Cell rollCell = row.getCell(rollColIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    rollNo = rollCell != null ? formatter.formatCellValue(rollCell) : null;
                }

                String email = null;
                if (emailColIdx != -1) {
                    Cell emailCell = row.getCell(emailColIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    email = emailCell != null ? formatter.formatCellValue(emailCell) : null;
                }

                String generatedId = "STU-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                boolean success = addStudent(generatedId, name, targetClassId, rollNo, email);
                if (success) {
                    count++;
                    // Add scores if present
                    for (int i = 0; i < subjectColIndices.size(); i++) {
                        int colIdx = subjectColIndices.get(i);
                        String subject = subjectNames.get(i);

                        Cell scoreCell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        if (scoreCell != null) {
                            try {
                                String scoreStr = formatter.formatCellValue(scoreCell);
                                if (!scoreStr.trim().isEmpty()) {
                                    double score = Double.parseDouble(scoreStr);
                                    addExamScore(generatedId, name, subject, score);
                                }
                            } catch (NumberFormatException nfe) {
                                System.err.println("Skipping invalid score for " + subject);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error importing Excel file: " + e.getMessage());
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Records a new exam score for a student.
     */
    public void addExamScore(String studentId, String studentName, String subjectName, double score) {
        dataManager.addSubjectScore(studentId, studentName, subjectName, score);
    }
}
