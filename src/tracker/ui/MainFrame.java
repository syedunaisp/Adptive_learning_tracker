package tracker.ui;

import tracker.data.DataManager;
import tracker.data.FileManager;
import tracker.model.Student;
import tracker.model.Subject;
import tracker.service.RecommendationEngine;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Main application window for the Adaptive Learning Progress Tracker.
 * Contains three tabs: Students, Subjects, and Reports.
 *
 * Delegates business logic to DataManager, FileManager, and
 * RecommendationEngine.
 * Keeps only presentation and event-handling logic inside this class.
 */
public class MainFrame extends JFrame {

    // --- Dependencies (injected via constructor or created internally) ---
    private final DataManager dataManager;
    private final FileManager fileManager;
    private final RecommendationEngine recommendationEngine;

    // --- Students Tab Components ---
    private JTextField txtStudentId;
    private JTextField txtStudentName;
    private JTextField txtSubject;
    private JTextField txtScore;
    private JButton btnAdd;
    private JTable studentTable;
    private DefaultTableModel studentTableModel;

    // --- Subjects Tab Components ---
    private JComboBox<String> cmbStudentSelector;
    private JTextArea txtWeakSubjects;
    private JTextArea txtRecommendations;
    private JButton btnRefreshSubjects;

    // --- Reports Tab Components ---
    private JButton btnShowAtRisk;
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JTextArea txtReportDetails;

    /**
     * Constructs the main application frame, initializing all dependencies
     * and building the complete GUI.
     */
    public MainFrame() {
        this.dataManager = new DataManager();
        this.fileManager = new FileManager();
        this.recommendationEngine = new RecommendationEngine();

        initializeFrame();
        buildUI();
    }

    /**
     * Sets basic frame properties: title, size, close behavior, and centering.
     */
    private void initializeFrame() {
        setTitle("Adaptive Learning Progress Tracker");
        setSize(950, 700);
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Builds the full UI with a JTabbedPane containing three tabs.
     */
    private void buildUI() {
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 13));

        tabbedPane.addTab("Students", buildStudentsTab());
        tabbedPane.addTab("Subjects", buildSubjectsTab());
        tabbedPane.addTab("Reports", buildReportsTab());

        // Refresh subjects tab data whenever the tab is selected
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex == 1) {
                refreshStudentSelector();
            }
        });

        add(tabbedPane);
    }

    // ==========================================
    // STUDENTS TAB
    // ==========================================

    /**
     * Builds the Students tab with input fields, an Add button, and a results
     * table.
     */
    private JPanel buildStudentsTab() {
        JPanel panel = new JPanel(new BorderLayout(StyleConstants.GAP_H, StyleConstants.GAP_V));
        panel.setBorder(StyleConstants.PANEL_PADDING);

        // --- Input Panel (top) ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new TitledBorder("Add Student / Subject"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = StyleConstants.FORM_INSETS;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Student ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(createLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtStudentId = createTextField();
        inputPanel.add(txtStudentId, gbc);

        // Row 0: Student Name
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(createLabel("Name:"), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtStudentName = createTextField();
        inputPanel.add(txtStudentName, gbc);

        // Row 1: Subject
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(createLabel("Subject:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtSubject = createTextField();
        inputPanel.add(txtSubject, gbc);

        // Row 1: Score
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        inputPanel.add(createLabel("Score:"), gbc);
        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        txtScore = createTextField();
        inputPanel.add(txtScore, gbc);

        // Row 2: Button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 0;

        // Use StyledButton to fix painting issues on Windows default L&F
        btnAdd = new StyledButton("Add / Submit");
        btnAdd.setFont(StyleConstants.BUTTON_FONT);
        btnAdd.setBackground(StyleConstants.PRIMARY);
        btnAdd.setForeground(StyleConstants.BUTTON_FG);
        btnAdd.setPreferredSize(StyleConstants.BUTTON_SIZE);
        inputPanel.add(btnAdd, gbc);

        panel.add(inputPanel, BorderLayout.NORTH);

        // --- Table (center) ---
        String[] columns = { "ID", "Name", "Average Score", "Status" };
        studentTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only table
            }
        };
        studentTable = new JTable(studentTableModel);
        setupTableStyles(studentTable);

        // Custom renderer for Status column (index 3)
        studentTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        JScrollPane scrollPane = new JScrollPane(studentTable);
        scrollPane.setBorder(new TitledBorder("Student Records"));

        panel.add(scrollPane, BorderLayout.CENTER);

        // --- Event listener ---
        btnAdd.addActionListener(e -> handleAddStudent());

        return panel;
    }

    /**
     * Handles the Add / Submit button click.
     * Validates input, creates or updates the student, saves the report.
     */
    private void handleAddStudent() {
        String id = txtStudentId.getText().trim();
        String name = txtStudentName.getText().trim();
        String subjectName = txtSubject.getText().trim();
        String scoreText = txtScore.getText().trim();

        // --- Input Validation ---
        if (id.isEmpty() || name.isEmpty() || subjectName.isEmpty() || scoreText.isEmpty()) {
            showError("All fields are required. Please fill in Student ID, Name, Subject, and Score.");
            return;
        }

        double score;
        try {
            score = Double.parseDouble(scoreText);
        } catch (NumberFormatException ex) {
            showError("Score must be a valid number (e.g., 75 or 82.5).");
            return;
        }

        if (score < 0 || score > 100) {
            showError("Score must be between 0 and 100.");
            return;
        }

        // --- Create or find student ---
        Student student = dataManager.findStudentById(id);
        boolean isNewStudent = (student == null);

        if (isNewStudent) {
            student = new Student(id, name);
            dataManager.addStudent(student);
        }

        // Add the subject
        Subject subject = new Subject(subjectName, score);
        student.addSubject(subject);

        // --- Generate recommendations and save report ---
        List<String> weakNames = student.getWeakSubjectNames();
        List<String> recommendations = recommendationEngine.getRecommendations(weakNames);

        try {
            fileManager.saveReport(student, recommendations);
        } catch (IOException ex) {
            showError("Failed to save report to file: " + ex.getMessage());
        }

        // --- Update table ---
        refreshStudentTable();

        // --- Clear input fields ---
        txtSubject.setText("");
        txtScore.setText("");

        // If new student, clear ID/Name too; if existing, keep them for adding more
        // subjects
        if (isNewStudent) {
            showInfo("Student '" + name + "' added with subject '" + subjectName + "' (Score: " + score
                    + ").\nReport saved to academic_report.txt.");
        } else {
            showInfo("Subject '" + subjectName + "' (Score: " + score + ") added to student '" + student.getName()
                    + "'.\nReport updated in academic_report.txt.");
        }
    }

    /**
     * Refreshes the student table to reflect the current state of the data store.
     */
    private void refreshStudentTable() {
        studentTableModel.setRowCount(0);
        for (Student student : dataManager.getStudents()) {
            String status = student.isAtRisk() ? "At Risk" : "Normal";
            Object[] row = {
                    student.getId(),
                    student.getName(),
                    String.format("%.2f", student.getAverageScore()),
                    status
            };
            studentTableModel.addRow(row);
        }
    }

    // ==========================================
    // SUBJECTS TAB
    // ==========================================

    /**
     * Builds the Subjects tab with a student selector, weak subjects display,
     * and generated recommendations.
     */
    private JPanel buildSubjectsTab() {
        JPanel panel = new JPanel(new BorderLayout(StyleConstants.GAP_H, StyleConstants.GAP_V));
        panel.setBorder(StyleConstants.PANEL_PADDING);

        // --- Top: Student selector ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(new TitledBorder("Select Student"));

        topPanel.add(createLabel("Student:"));
        cmbStudentSelector = new JComboBox<>();
        cmbStudentSelector.setFont(StyleConstants.BODY_FONT);
        cmbStudentSelector.setPreferredSize(new Dimension(300, StyleConstants.COMBO_HEIGHT));
        topPanel.add(cmbStudentSelector);

        btnRefreshSubjects = new StyledButton("Show Details");
        btnRefreshSubjects.setFont(StyleConstants.BUTTON_FONT);
        btnRefreshSubjects.setBackground(StyleConstants.PRIMARY);
        btnRefreshSubjects.setForeground(StyleConstants.BUTTON_FG);
        topPanel.add(btnRefreshSubjects);

        panel.add(topPanel, BorderLayout.NORTH);

        // --- Center: Weak subjects + Recommendations ---
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Left: Weak subjects
        txtWeakSubjects = createTextArea();
        JScrollPane weakScroll = new JScrollPane(txtWeakSubjects);
        weakScroll.setBorder(new TitledBorder("Weak Subjects (Score < 60)"));
        centerPanel.add(weakScroll);

        // Right: Recommendations
        txtRecommendations = createTextArea();
        JScrollPane recScroll = new JScrollPane(txtRecommendations);
        recScroll.setBorder(new TitledBorder("Learning Recommendations"));
        centerPanel.add(recScroll);

        panel.add(centerPanel, BorderLayout.CENTER);

        // --- Event listener ---
        btnRefreshSubjects.addActionListener(e -> handleShowSubjectDetails());

        return panel;
    }

    /**
     * Populates the student selector combo box with current students.
     */
    private void refreshStudentSelector() {
        cmbStudentSelector.removeAllItems();
        List<Student> students = dataManager.getStudents();
        if (students.isEmpty()) {
            cmbStudentSelector.addItem("-- No students added yet --");
        } else {
            for (Student student : students) {
                cmbStudentSelector.addItem(student.getId() + " - " + student.getName());
            }
        }
    }

    /**
     * Handles the Show Details button on the Subjects tab.
     * Displays weak subjects and recommendations for the selected student.
     */
    private void handleShowSubjectDetails() {
        List<Student> students = dataManager.getStudents();
        if (students.isEmpty()) {
            txtWeakSubjects.setText("No students available.\nPlease add students in the Students tab first.");
            txtRecommendations.setText("");
            return;
        }

        int selectedIndex = cmbStudentSelector.getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= students.size()) {
            txtWeakSubjects.setText("Please select a valid student.");
            txtRecommendations.setText("");
            return;
        }

        Student student = students.get(selectedIndex);

        // --- Display all subjects ---
        StringBuilder sbSubjects = new StringBuilder();
        sbSubjects.append("Student: ").append(student.getName());
        sbSubjects.append(" (ID: ").append(student.getId()).append(")\n");
        sbSubjects.append("Average Score: ").append(String.format("%.2f", student.getAverageScore())).append("\n");
        sbSubjects.append("Status: ").append(student.isAtRisk() ? "AT RISK" : "Normal").append("\n");
        sbSubjects.append("\n--- All Subjects ---\n");

        for (Subject subject1 : student.getSubjects()) {
            String marker = subject1.getScore() < 60 ? " [WEAK]" : "";
            sbSubjects.append(String.format("  %-20s : %6.2f%s\n",
                    subject1.getSubjectName(), subject1.getScore(), marker));
        }

        // --- Weak subjects summary ---
        List<Subject> weakSubjects = student.getWeakSubjects();
        if (weakSubjects.isEmpty()) {
            sbSubjects.append("\nNo weak subjects! All scores are 60 or above.");
        } else {
            sbSubjects.append("\n--- Weak Subjects ---\n");
            for (Subject ws : weakSubjects) {
                sbSubjects.append(String.format("  %-20s : %6.2f\n", ws.getSubjectName(), ws.getScore()));
            }
        }
        txtWeakSubjects.setText(sbSubjects.toString());

        // --- Recommendations ---
        List<String> weakNames = student.getWeakSubjectNames();
        List<String> recommendations = recommendationEngine.getRecommendations(weakNames);

        StringBuilder sbRec = new StringBuilder();
        sbRec.append("Recommendations for ").append(student.getName()).append(":\n\n");
        for (String rec : recommendations) {
            sbRec.append(rec).append("\n");
        }
        txtRecommendations.setText(sbRec.toString());
    }

    // ==========================================
    // REPORTS TAB
    // ==========================================

    /**
     * Builds the Reports tab with an At-Risk filter button and display area.
     */
    private JPanel buildReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(StyleConstants.GAP_H, StyleConstants.GAP_V));
        panel.setBorder(StyleConstants.PANEL_PADDING);

        // --- Top: Button ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(new TitledBorder("Actions"));

        btnShowAtRisk = new StyledButton("Show At-Risk Students (Average < 50)");
        btnShowAtRisk.setFont(StyleConstants.BUTTON_FONT);
        btnShowAtRisk.setBackground(StyleConstants.PRIMARY);
        btnShowAtRisk.setForeground(StyleConstants.BUTTON_FG);
        btnShowAtRisk.setPreferredSize(StyleConstants.BUTTON_SIZE_WIDE);
        topPanel.add(btnShowAtRisk);
        panel.add(topPanel, BorderLayout.NORTH);

        // --- Center: Split between table and detail ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));

        // Table for at-risk students
        String[] columns = { "ID", "Name", "Average Score", "Status" };
        reportTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        reportTable = new JTable(reportTableModel);
        setupTableStyles(reportTable);
        // Custom renderer for Status (red only)
        reportTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        JScrollPane tableScroll = new JScrollPane(reportTable);
        tableScroll.setBorder(new TitledBorder("At-Risk Students"));
        tableScroll.setPreferredSize(new Dimension(0, 200));
        centerPanel.add(tableScroll, BorderLayout.NORTH);

        // Detail text area
        txtReportDetails = createTextArea();
        JScrollPane detailScroll = new JScrollPane(txtReportDetails);
        detailScroll.setBorder(new TitledBorder("Detailed Report"));
        centerPanel.add(detailScroll, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // --- Event listener ---
        btnShowAtRisk.addActionListener(e -> handleShowAtRisk());

        // Table row selection listener -- show details for selected at-risk student
        reportTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                handleAtRiskRowSelected();
            }
        });

        return panel;
    }

    /**
     * Handles the Show At-Risk Students button.
     * Filters and displays students with average score below 50.
     */
    private void handleShowAtRisk() {
        List<Student> atRisk = dataManager.getAtRiskStudents();

        reportTableModel.setRowCount(0);

        if (atRisk.isEmpty()) {
            txtReportDetails.setText("No at-risk students found.\n\n" +
                    "All students currently have an average score of 50 or above,\n" +
                    "or no students have been added yet.");
            return;
        }

        for (Student student : atRisk) {
            Object[] row = {
                    student.getId(),
                    student.getName(),
                    String.format("%.2f", student.getAverageScore()),
                    "At Risk"
            };
            reportTableModel.addRow(row);
        }

        txtReportDetails.setText("Found " + atRisk.size() + " at-risk student(s).\n" +
                "Select a row above to view detailed report.\n\n" +
                "At-Risk Criteria: Average Score < 50");
    }

    /**
     * Handles row selection in the at-risk table to show detailed report.
     */
    private void handleAtRiskRowSelected() {
        int selectedRow = reportTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        String id = (String) reportTableModel.getValueAt(selectedRow, 0);
        Student student = dataManager.findStudentById(id);
        if (student == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("============================================\n");
        sb.append("       DETAILED REPORT\n");
        sb.append("============================================\n");
        sb.append("Student Name : ").append(student.getName()).append("\n");
        sb.append("Student ID   : ").append(student.getId()).append("\n");
        sb.append("Average Score: ").append(String.format("%.2f", student.getAverageScore())).append("\n");
        sb.append("Status       : AT RISK\n\n");

        sb.append("--- Subjects ---\n");
        for (Subject subject1 : student.getSubjects()) {
            String marker = subject1.getScore() < 60 ? " [WEAK]" : "";
            sb.append(String.format("  %-20s : %6.2f%s\n",
                    subject1.getSubjectName(), subject1.getScore(), marker));
        }

        sb.append("\n--- Weak Subjects ---\n");
        List<Subject> weakSubjects = student.getWeakSubjects();
        if (weakSubjects.isEmpty()) {
            sb.append("  None\n");
        } else {
            for (Subject ws : weakSubjects) {
                sb.append(String.format("  %-20s : %6.2f\n", ws.getSubjectName(), ws.getScore()));
            }
        }

        sb.append("\n--- Recommendations ---\n");
        List<String> weakNames = student.getWeakSubjectNames();
        List<String> recommendations = recommendationEngine.getRecommendations(weakNames);
        for (String rec : recommendations) {
            sb.append(rec).append("\n");
        }
        sb.append("============================================\n");

        txtReportDetails.setText(sb.toString());
        txtReportDetails.setCaretPosition(0);
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    /**
     * Shows an error dialog to the user.
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows an informational dialog to the user.
     */
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==========================================
    // STYLE HELPERS
    // ==========================================

    private JLabel createLabel(String text) {
        return new JLabel(text); // Font/Color handled by UIManager
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField(15);
        textField.setMargin(StyleConstants.TEXT_FIELD_MARGIN);
        return textField;
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setMargin(new Insets(5, 5, 5, 5));
        return area;
    }

    private void setupTableStyles(JTable table) {
        table.setShowGrid(true);
        table.setSelectionBackground(StyleConstants.PRIMARY);
        table.setSelectionForeground(Color.WHITE);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align the Average Score (index 2)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    }

    /**
     * Custom renderer to color-code the Status column.
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected && value != null) {
                String status = value.toString();
                if ("At Risk".equals(status)) {
                    setForeground(StyleConstants.ACCENT_RED);
                    setFont(StyleConstants.TABLE_BODY_FONT.deriveFont(Font.BOLD));
                } else if ("Normal".equals(status)) {
                    setForeground(StyleConstants.ACCENT_GREEN);
                    setFont(StyleConstants.TABLE_BODY_FONT.deriveFont(Font.BOLD));
                } else {
                    setForeground(Color.BLACK);
                }
            }
            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }
}
