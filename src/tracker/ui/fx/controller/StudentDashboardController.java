package tracker.ui.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tracker.data.DataManager;
import tracker.data.dao.GoalDAO;
import tracker.data.dao.ProfileDAO;
import tracker.data.dao.ReevaluationDAO;
import tracker.model.*;
import tracker.security.SessionManager;
import tracker.service.*;
import tracker.service.ai.RiskPredictor;
import tracker.service.ai.TrendAnalyzer;
import tracker.ui.fx.ViewManager;
import tracker.ui.fx.ViewManagerAware;

import java.util.List;

/**
 * Controller for the Student Dashboard.
 * Manages overview + Goals, Recommendations, Re-evaluation, Learning Profile
 * panels.
 */
public class StudentDashboardController implements ViewManagerAware {

    @FXML
    private Label welcomeLabel;
    @FXML
    private Label avgScoreLabel;
    @FXML
    private Label riskLevelLabel;
    @FXML
    private Label trendLabel;
    @FXML
    private Label percentileLabel;
    @FXML
    private TableView<Subject> subjectsTable;
    @FXML
    private TableColumn<Subject, String> subjectNameCol;
    @FXML
    private TableColumn<Subject, String> subjectScoreCol;
    @FXML
    private TableColumn<Subject, String> subjectStatusCol;
    @FXML
    private HBox riskCardsContainer;
    @FXML
    private FlowPane gapsContainer;
    @FXML
    private StackPane contentArea;
    @FXML
    private VBox overviewPane;

    private ViewManager viewManager;
    private final DataManager dataManager = new DataManager();
    private final TrendAnalyzer trendAnalyzer = new TrendAnalyzer();
    private final RiskPredictor riskPredictor = new RiskPredictor(trendAnalyzer);
    private final AnalyticsService analyticsService = new AnalyticsService(riskPredictor, trendAnalyzer);
    private final GoalTracker goalTracker = new GoalTracker(new GoalDAO());
    private final ReevaluationWorkflow reevalWorkflow = new ReevaluationWorkflow(new ReevaluationDAO());
    private final ProfileService profileService = new ProfileService(new ProfileDAO());

    // Cached panels
    private VBox goalsPane;
    private VBox recommendationsPane;
    private VBox reevalPane;
    private VBox profilePane;

    // Cached student ref
    private Student currentStudent;
    private int currentStudentDbId = -1;

    @Override
    public void setViewManager(ViewManager viewManager) {
        this.viewManager = viewManager;
        loadDashboardData();
    }

    @FXML
    private void initialize() {
        subjectsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tracker.ui.fx.util.TableColumnFormatter.bindColumnWidth(subjectNameCol, subjectsTable, 0.40);
        tracker.ui.fx.util.TableColumnFormatter.bindColumnWidth(subjectScoreCol, subjectsTable, 0.30);
        tracker.ui.fx.util.TableColumnFormatter.bindColumnWidth(subjectStatusCol, subjectsTable, 0.30);

        subjectNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getSubjectName()));
        subjectScoreCol
                .setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.1f", cd.getValue().getScore())));
        subjectStatusCol.setCellValueFactory(cd -> {
            double score = cd.getValue().getScore();
            String status = score >= 80 ? "✓ Excellent" : score >= 60 ? "○ Passing" : "✗ Needs Work";
            return new SimpleStringProperty(status);
        });
    }

    private void loadDashboardData() {
        // Always refresh from DB to pick up changes made by other users (teachers)
        dataManager.refreshCache();

        String username = viewManager.getCurrentUsername();
        welcomeLabel.setText("Welcome, " + (username != null ? username : "Student"));

        String linkedStudentId = SessionManager.getLinkedStudentId();
        currentStudentDbId = viewManager.getCurrentStudentDbId();

        if (linkedStudentId == null) {
            avgScoreLabel.setText("N/A");
            riskLevelLabel.setText("N/A");
            percentileLabel.setText("N/A");
            riskCardsContainer.getChildren().clear();
            gapsContainer.getChildren().clear();
            riskCardsContainer.getChildren().add(new Label("No student profile linked to this account."));
            return;
        }

        List<Student> allStudents = dataManager.getStudents();
        for (Student s : allStudents) {
            if (linkedStudentId.equals(s.getId())) {
                currentStudent = s;
                break;
            }
        }

        if (currentStudent == null || currentStudent.getSubjects().isEmpty()) {
            avgScoreLabel.setText("N/A");
            riskLevelLabel.setText("No Data");
            trendLabel.setText("—");
            percentileLabel.setText("—");
            return;
        }

        double avg = currentStudent.getAverageScore();
        avgScoreLabel.setText(String.format("%.1f", avg));

        RiskScore risk = riskPredictor.assessRisk(currentStudent);
        riskLevelLabel.setText(risk.getLevel().getLabel());
        String riskColor = risk.getLevel() == RiskScore.Level.HIGH ? "#ff4757"
                : risk.getLevel() == RiskScore.Level.MODERATE ? "#ffa502" : "#2ed573";
        riskLevelLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + riskColor + ";");

        trendAnalyzer.recordAverage(currentStudent.getId(), avg);
        TrendDirection trend = trendAnalyzer.getTrend(currentStudent.getId());
        String trendIcon = trend == TrendDirection.IMPROVING ? "↑ Improving"
                : trend == TrendDirection.DECLINING ? "↓ Declining" : "→ Stable";
        String trendColor = trend == TrendDirection.IMPROVING ? "#2ed573"
                : trend == TrendDirection.DECLINING ? "#ff4757" : "#ffa502";
        trendLabel.setText(trendIcon);
        trendLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + trendColor + ";");

        double percentile = analyticsService.getStudentPercentile(currentStudent, allStudents);
        percentileLabel.setText(String.format("%.0f%%", percentile));

        subjectsTable.setItems(FXCollections.observableArrayList(currentStudent.getSubjects()));

        riskCardsContainer.getChildren().clear();
        gapsContainer.getChildren().clear();

        // Risk Factor Breakdown mini-cards
        riskCardsContainer.getChildren().addAll(
                createMiniCard("Average Score Focus", String.format("%.1f%%", risk.getAverageContributionPercent()),
                        "#ff4757"),
                createMiniCard("Weak Subject Count", String.valueOf(currentStudent.getWeakSubjects().size()),
                        "#ffa502"),
                createMiniCard("Lowest Score Factor", String.format("%.1f%%", risk.getLowestContributionPercent()),
                        "#ff6348"),
                createMiniCard("Trend Factor", String.format("%.1f%%", risk.getTrendContributionPercent()), "#3742fa"));

        // Learning Gaps
        List<String> gaps = analyticsService.identifyLearningGaps(currentStudent);
        if (gaps.isEmpty()) {
            Label noGaps = new Label("No learning gaps detected. Keep up the great work! 🎉");
            noGaps.setStyle("-fx-text-fill: #2ed573;");
            gapsContainer.getChildren().add(noGaps);
        } else {
            for (String gap : gaps) {
                HBox gapCard = new HBox(8);
                gapCard.setAlignment(Pos.CENTER_LEFT);
                gapCard.setStyle(
                        "-fx-background-color: #ff475720; -fx-background-radius: 8; -fx-padding: 8 12; -fx-border-color: #ff4757; -fx-border-radius: 8;");
                Label gapIcon = new Label("❗");
                Label gapText = new Label(gap);
                gapText.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                gapCard.getChildren().addAll(gapIcon, gapText);
                gapsContainer.getChildren().add(gapCard);
            }
        }
    }

    private VBox createMiniCard(String title, String value, String color) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS); // Stretch to fill space

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("secondary");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    // =========================================================================
    // Sidebar Navigation
    // =========================================================================

    @FXML
    private void showOverview() {
        contentArea.getChildren().forEach(n -> n.setVisible(false));
        overviewPane.setVisible(true);
    }

    @FXML
    private void showGoals() {
        if (goalsPane == null) {
            goalsPane = buildGoalsPanel();
            contentArea.getChildren().add(goalsPane);
        }
        contentArea.getChildren().forEach(n -> n.setVisible(false));
        refreshGoalsPanel();
        goalsPane.setVisible(true);
    }

    @FXML
    private void showRecommendations() {
        if (recommendationsPane == null) {
            recommendationsPane = buildRecommendationsPanel();
            contentArea.getChildren().add(recommendationsPane);
        }
        contentArea.getChildren().forEach(n -> n.setVisible(false));
        recommendationsPane.setVisible(true);
    }

    @FXML
    private void showReevaluation() {
        if (reevalPane == null) {
            reevalPane = buildReevalPanel();
            contentArea.getChildren().add(reevalPane);
        }
        contentArea.getChildren().forEach(n -> n.setVisible(false));
        refreshReevalPanel();
        reevalPane.setVisible(true);
    }

    @FXML
    private void showProfile() {
        if (profilePane == null) {
            profilePane = buildProfilePanel();
            contentArea.getChildren().add(profilePane);
        }
        contentArea.getChildren().forEach(n -> n.setVisible(false));
        profilePane.setVisible(true);
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        viewManager.clearSession();
        viewManager.showLogin();
    }

    // =========================================================================
    // GOALS PANEL
    // =========================================================================

    private VBox goalsListBox;

    private VBox buildGoalsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        // Header
        Label title = new Label("🎯 My Academic Goals");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Set new goal form
        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("card");

        Label formTitle = new Label("Set a New Goal");
        formTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        HBox formRow = new HBox(12);
        formRow.setAlignment(Pos.CENTER_LEFT);
        TextField targetField = new TextField();
        targetField.setPromptText("Target score (0-100)");
        targetField.setPrefWidth(180);
        TextField deadlineField = new TextField();
        deadlineField.setPromptText("Deadline (YYYY-MM-DD)");
        deadlineField.setPrefWidth(180);
        Button setGoalBtn = new Button("Set Goal");
        setGoalBtn.getStyleClass().add("button-primary");
        Label goalMsg = new Label();

        setGoalBtn.setOnAction(e -> {
            try {
                double target = Double.parseDouble(targetField.getText().trim());
                String deadline = deadlineField.getText().trim().isEmpty() ? null : deadlineField.getText().trim();
                if (currentStudentDbId > 0 && goalTracker.setGoal(currentStudentDbId, null, target, deadline)) {
                    goalMsg.setText("✓ Goal set!");
                    goalMsg.getStyleClass().setAll("success-label");
                    targetField.clear();
                    deadlineField.clear();
                    refreshGoalsPanel();
                } else {
                    goalMsg.setText("Failed to set goal.");
                    goalMsg.getStyleClass().setAll("error-label");
                }
            } catch (NumberFormatException ex) {
                goalMsg.setText("Enter a valid number.");
                goalMsg.getStyleClass().setAll("error-label");
            }
        });

        formRow.getChildren().addAll(targetField, deadlineField, setGoalBtn);
        formCard.getChildren().addAll(formTitle, formRow, goalMsg);

        // Active goals list
        VBox goalsCard = new VBox(12);
        goalsCard.getStyleClass().add("card");
        Label goalsTitle = new Label("Active Goals");
        goalsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        goalsListBox = new VBox(16);
        goalsCard.getChildren().addAll(goalsTitle, goalsListBox);

        panel.getChildren().addAll(title, formCard, goalsCard);
        return panel;
    }

    private void refreshGoalsPanel() {
        if (goalsListBox == null || currentStudentDbId < 0)
            return;
        List<Goal> goals = goalTracker.getAllGoals(currentStudentDbId);
        goalsListBox.getChildren().clear();
        if (goals.isEmpty()) {
            Label empty = new Label("No goals set yet. Set your first academic target above!");
            goalsListBox.getChildren().add(empty);
        } else {
            for (Goal g : goals) {
                VBox goalItem = new VBox(8);
                goalItem.setStyle("-fx-padding: 12; -fx-background-color: #2f3640; -fx-background-radius: 8;");

                HBox topRow = new HBox();
                Label targetLabel = new Label("Target: " + g.getTargetScore());
                targetLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label statusBadge = new Label(g.getStatus());
                statusBadge.setStyle(
                        "-fx-padding: 2 8; -fx-background-radius: 12; -fx-font-weight: bold; -fx-text-fill: white; " +
                                ("ACHIEVED".equalsIgnoreCase(g.getStatus()) ? "-fx-background-color: #2ed573;"
                                        : "-fx-background-color: #ffa502;"));
                topRow.getChildren().addAll(targetLabel, spacer, statusBadge);

                double avg = currentStudent != null ? currentStudent.getAverageScore() : 0.0;
                double progress = g.getTargetScore() > 0 ? Math.min(1.0, avg / g.getTargetScore()) : 0;

                ProgressBar pb = new ProgressBar(progress);
                pb.setMaxWidth(Double.MAX_VALUE);
                pb.setStyle("-fx-accent: #6c63ff;");
                if (progress >= 1.0)
                    pb.setStyle("-fx-accent: #2ed573;");

                Label bottomLabel = new Label(String.format("Current Average: %.1f | Deadline: %s", avg,
                        g.getDeadline() != null ? g.getDeadline() : "None"));
                bottomLabel.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 12px;");

                goalItem.getChildren().addAll(topRow, pb, bottomLabel);
                goalsListBox.getChildren().add(goalItem);
            }
        }
    }

    // =========================================================================
    // RECOMMENDATIONS PANEL
    // =========================================================================

    private VBox buildRecommendationsPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("💡 AI Recommendations");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        GridPane recsContainer = new GridPane();
        recsContainer.setHgap(16);
        recsContainer.setVgap(16);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        recsContainer.getColumnConstraints().addAll(col1, col2);

        if (currentStudent != null) {
            tracker.service.ai.AdaptivePlanner planner = new tracker.service.ai.AdaptivePlanner();
            RiskScore risk = riskPredictor.assessRisk(currentStudent);
            TrendDirection trend = trendAnalyzer.getTrend(currentStudent.getId());
            List<String> recs = planner.generateRecommendations(currentStudent, risk, trend);

            if (recs.isEmpty()) {
                VBox emptyCard = new VBox(12);
                emptyCard.getStyleClass().add("card");
                Label noRecs = new Label("No specific recommendations at this time. You're doing great! 🎉");
                noRecs.setWrapText(true);
                noRecs.setStyle("-fx-text-fill: #2ed573;");
                emptyCard.getChildren().add(noRecs);
                recsContainer.getChildren().add(emptyCard);
            } else {
                VBox currentCard = null;
                VBox currentBulletList = null;

                int col = 0;
                int row = 0;

                for (String line : recs) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("---"))
                        continue;

                    boolean isHeader = line.startsWith("[");
                    boolean isAction = line.startsWith("->") || line.startsWith("-");

                    if (isHeader || currentCard == null) {
                        currentCard = new VBox(12);
                        currentCard.getStyleClass().add("card");
                        currentCard.setMaxWidth(Double.MAX_VALUE);

                        String titleText = line.replaceAll("^-+|-+$", "").trim();

                        String titleColor = "white";
                        if (titleText.startsWith("[CRITICAL]") || titleText.startsWith("[WARNING]"))
                            titleColor = "#ff4757";
                        else if (titleText.startsWith("[INFO]") || titleText.startsWith("[STRENGTH]"))
                            titleColor = "#2ed573";
                        else if (titleText.startsWith("[CATEGORY ALERT]"))
                            titleColor = "#ffa502";
                        else if (titleText.startsWith("[TREND]"))
                            titleColor = "#3742fa";
                        else if (titleText.startsWith("[ACTION]"))
                            titleColor = "#00cec9";

                        Label titleLabel = new Label(titleText);
                        titleLabel.setWrapText(true);
                        titleLabel.setStyle(
                                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + titleColor + ";");

                        currentBulletList = new VBox(8);
                        currentCard.getChildren().addAll(titleLabel, currentBulletList);

                        recsContainer.add(currentCard, col, row);
                        col++;
                        if (col > 1) {
                            col = 0;
                            row++;
                        }
                    } else if (isAction) {
                        HBox actionRow = new HBox(8);
                        actionRow.setAlignment(Pos.TOP_LEFT);
                        Label dot = new Label("•");
                        dot.setStyle("-fx-text-fill: #b2bec3; -fx-font-size: 14px; -fx-font-weight: bold;");

                        String actionText = line.startsWith("->") ? line.substring(2).trim() : line.substring(1).trim();
                        Label text = new Label(actionText);
                        text.setWrapText(true);
                        text.setStyle("-fx-text-fill: #dfe6e9; -fx-font-size: 13px;");

                        actionRow.getChildren().addAll(dot, text);
                        currentBulletList.getChildren().add(actionRow);
                    } else {
                        Label text = new Label(line);
                        text.setWrapText(true);
                        text.setStyle("-fx-text-fill: #dfe6e9; -fx-font-size: 13px;");
                        currentBulletList.getChildren().add(text);
                    }
                }
            }
        } else {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.getChildren().add(new Label("No student data available."));
            recsContainer.getChildren().add(emptyCard);
        }

        // Peer comparison
        VBox peerCard = new VBox(12);
        peerCard.getStyleClass().add("card");
        Label peerTitle = new Label("Peer Comparison");
        peerTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        if (currentStudent != null) {
            List<Student> all = dataManager.getStudents();
            java.util.Map<String, Double> peer = analyticsService.getPeerComparison(currentStudent, all);

            HBox peerMetricsContainer = new HBox(16);
            peerMetricsContainer.getChildren().addAll(
                    createMiniCard("Your Average", String.format("%.1f", peer.getOrDefault("studentAvg", 0.0)),
                            "#2ed573"),
                    createMiniCard("Class Average", String.format("%.1f", peer.getOrDefault("classAvg", 0.0)),
                            "#ffa502"),
                    createMiniCard("Top 10% Average", String.format("%.1f", peer.getOrDefault("top10Avg", 0.0)),
                            "#3742fa"),
                    createMiniCard("Your Percentile", String.format("%.0f%%", peer.getOrDefault("percentile", 0.0)),
                            "#00cec9"));

            peerCard.getChildren().addAll(peerTitle, peerMetricsContainer);
        } else {
            peerCard.getChildren().addAll(peerTitle, new Label("No data available."));
        }

        panel.getChildren().addAll(title, recsContainer, peerCard);
        return panel;
    }

    // =========================================================================
    // RE-EVALUATION PANEL
    // =========================================================================

    private VBox reevalHistoryBox;

    private VBox buildReevalPanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("📝 Re-evaluation Requests");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Submit form
        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("card");
        Label formTitle = new Label("Submit New Request");
        formTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");

        // Subject dropdown
        Label subjectLabel = new Label("Subject");
        subjectLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
        javafx.scene.control.ComboBox<String> subjectDropdown = new javafx.scene.control.ComboBox<>();
        subjectDropdown.setMaxWidth(Double.MAX_VALUE);
        subjectDropdown.setPromptText("Select subject...");
        if (currentStudent != null) {
            for (tracker.model.Subject sub : currentStudent.getSubjects()) {
                subjectDropdown.getItems().add(sub.getSubjectName());
            }
        }

        // Reason
        Label reasonLabel = new Label("Reason for Re-evaluation");
        reasonLabel.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 13px;");
        TextArea reasonArea = new TextArea();
        reasonArea.setPromptText("Explain why you need a re-evaluation (min 10 characters)...");
        reasonArea.setPrefHeight(80);
        reasonArea.setWrapText(true);
        reasonArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #e0e0e8;");

        Button submitBtn = new Button("Submit Request");
        submitBtn.getStyleClass().add("button-primary");
        Label reevalMsg = new Label();

        submitBtn.setOnAction(e -> {
            String selectedSubject = subjectDropdown.getValue();
            String reason = reasonArea.getText();
            if (selectedSubject == null || selectedSubject.isEmpty()) {
                reevalMsg.setText("Please select a subject.");
                reevalMsg.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                return;
            }
            int teacherId = reevalWorkflow.getStudentTeacherId(currentStudentDbId);
            if (teacherId <= 0) {
                reevalMsg.setText("No teacher found for your class. Contact admin.");
                reevalMsg.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
                return;
            }
            if (currentStudentDbId > 0
                    && reevalWorkflow.submitRequest(selectedSubject, currentStudentDbId, reason, teacherId)) {
                reevalMsg.setText("✓ Request submitted!");
                reevalMsg.setStyle("-fx-text-fill: #2ed573; -fx-font-weight: bold;");
                subjectDropdown.setValue(null);
                reasonArea.clear();
                refreshReevalPanel();
            } else {
                reevalMsg.setText("Failed to submit. Ensure reason is at least 10 characters.");
                reevalMsg.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");
            }
        });

        formCard.getChildren().addAll(formTitle, subjectLabel, subjectDropdown, reasonLabel, reasonArea, submitBtn,
                reevalMsg);

        // History
        VBox histCard = new VBox(12);
        histCard.getStyleClass().add("card");
        Label histTitle = new Label("My Requests");
        histTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        reevalHistoryBox = new VBox(12);
        histCard.getChildren().addAll(histTitle, reevalHistoryBox);

        panel.getChildren().addAll(title, formCard, histCard);
        return panel;
    }

    private void refreshReevalPanel() {
        if (reevalHistoryBox == null || currentStudentDbId < 0)
            return;
        reevalHistoryBox.getChildren().clear();
        List<ReevaluationRequest> requests = reevalWorkflow.getStudentRequests(currentStudentDbId);
        if (requests.isEmpty()) {
            reevalHistoryBox.getChildren().add(new Label("No re-evaluation requests submitted."));
        } else {
            for (ReevaluationRequest r : requests) {
                VBox reqCard = new VBox(6);
                reqCard.setStyle("-fx-background-color: #2f3542; -fx-background-radius: 8; -fx-padding: 12;");

                String subjectText = r.getSubjectName() != null ? r.getSubjectName() : "N/A";
                Label subjectLbl = new Label("📚 " + subjectText);
                subjectLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #70a1ff;");

                String statusColor = "PENDING".equals(r.getStatus()) ? "#ffa502"
                        : "RESOLVED".equals(r.getStatus()) ? "#2ed573" : "#ff4757";
                Label statusLbl = new Label("Status: " + r.getStatus());
                statusLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + statusColor + ";");

                Label reasonLbl = new Label("Reason: " + r.getReason());
                reasonLbl.setWrapText(true);

                reqCard.getChildren().addAll(subjectLbl, statusLbl, reasonLbl);

                if ("RESOLVED".equals(r.getStatus()) || "REJECTED".equals(r.getStatus())) {
                    if (r.getResolutionNotes() != null && !r.getResolutionNotes().isEmpty()) {
                        Label notesLbl = new Label("💬 Teacher: " + r.getResolutionNotes());
                        notesLbl.setWrapText(true);
                        notesLbl.setStyle("-fx-text-fill: #dfe4ea;");
                        reqCard.getChildren().add(notesLbl);
                    }
                    if (r.getUpdatedMarks() != null) {
                        Label marksLbl = new Label("✅ Updated Marks: " + String.format("%.1f", r.getUpdatedMarks()));
                        marksLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #2ed573;");
                        reqCard.getChildren().add(marksLbl);
                    }
                }

                reevalHistoryBox.getChildren().add(reqCard);
            }
        }
    }

    // =========================================================================
    // LEARNING PROFILE PANEL
    // =========================================================================

    private VBox buildProfilePanel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0));

        Label title = new Label("🧠 Learning Profile");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");

        VBox contentBox = new VBox(16);

        if (currentStudentDbId > 0) {
            tracker.model.LearningProfile profile = profileService.getStudentProfile(currentStudentDbId);
            if (profile != null) {
                contentBox.getChildren().add(buildStrategyDashboard(profile));
            } else {
                contentBox.getChildren().add(buildQuestionnaireUI());
            }
        } else {
            VBox emptyCard = new VBox(12);
            emptyCard.getStyleClass().add("card");
            emptyCard.getChildren().add(new Label("No student account linked."));
            contentBox.getChildren().add(emptyCard);
        }

        panel.getChildren().addAll(title, contentBox);
        return panel;
    }

    private VBox buildQuestionnaireUI() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");

        Label header = new Label("Study Strategy Advisor");
        header.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label subHeader = new Label("Discover Your Optimal Strategy by answering a few quick questions.");
        subHeader.setStyle("-fx-text-fill: #a4b0be; -fx-font-size: 14px;");

        VBox questionsContainer = new VBox(24);
        List<String[]> questions = profileService.getQuestionnaireQuestions();

        // Keep track of the comboboxes to read answers later
        java.util.Map<Integer, javafx.scene.control.ComboBox<String>> answerMap = new java.util.HashMap<>();

        for (int i = 0; i < questions.size(); i++) {
            String[] qObj = questions.get(i);
            int qId = Integer.parseInt(qObj[0]);
            String qText = qObj[1];
            String optA = qObj[2];
            String optB = qObj[3];
            String optC = qObj[4];
            String optD = qObj[5];

            VBox row = new VBox(8);
            Label qLabel = new Label((i + 1) + ". " + qText);
            qLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: white;");
            qLabel.setWrapText(true);

            javafx.scene.control.ComboBox<String> dropdown = new javafx.scene.control.ComboBox<>();
            dropdown.getItems().addAll(optA, optB, optC, optD);
            dropdown.setMaxWidth(Double.MAX_VALUE);
            dropdown.setPromptText("Select an option...");
            dropdown.setStyle(
                    "-fx-font-size: 14px; -fx-background-color: #2f3542; -fx-text-fill: white; -fx-border-color: #57606f; -fx-border-radius: 4;");

            answerMap.put(qId, dropdown);
            row.getChildren().addAll(qLabel, dropdown);
            questionsContainer.getChildren().add(row);
        }

        Button analyzeBtn = new Button("Analyze My Study Strategy");
        analyzeBtn.setMaxWidth(Double.MAX_VALUE);
        analyzeBtn.setStyle(
                "-fx-background-color: #3742fa; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 12 0; -fx-cursor: hand;");

        Label errorMsg = new Label("");
        errorMsg.setStyle("-fx-text-fill: #ff4757; -fx-font-weight: bold;");

        analyzeBtn.setOnAction(e -> {
            java.util.Map<Integer, String> finalAnswers = new java.util.HashMap<>();
            for (java.util.Map.Entry<Integer, javafx.scene.control.ComboBox<String>> entry : answerMap.entrySet()) {
                int selectionIndex = entry.getValue().getSelectionModel().getSelectedIndex();
                if (selectionIndex == -1) {
                    errorMsg.setText("Please answer all questions before analyzing.");
                    return;
                }
                String letter = selectionIndex == 0 ? "A" : selectionIndex == 1 ? "B" : selectionIndex == 2 ? "C" : "D";
                finalAnswers.put(entry.getKey(), letter);
            }

            profileService.determineProfile(currentStudentDbId, finalAnswers);
            contentArea.getChildren().remove(profilePane);
            profilePane = null;
            showProfile();
            loadDashboardData(); // Refresh any top-level metrics
        });

        card.getChildren().addAll(header, subHeader, questionsContainer, errorMsg, analyzeBtn);
        return card;
    }

    private VBox buildStrategyDashboard(tracker.model.LearningProfile profile) {
        VBox layout = new VBox(20);

        // Header Card focusing on Description instead of the crude Name
        VBox headerCard = new VBox(8);
        headerCard.getStyleClass().add("card");
        headerCard.setStyle(
                "-fx-background-color: linear-gradient(to right, #2f3542, #1e272e); -fx-background-radius: 12; -fx-padding: 24;");

        Label dashTitle = new Label("Your Personalized Strategy Dashboard");
        dashTitle.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #00cec9;");

        Label descLabel = new Label(profile.getDescription());
        descLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #dfe4ea; -fx-line-spacing: 0.5em;");
        descLabel.setWrapText(true);

        headerCard.getChildren().addAll(dashTitle, descLabel);
        layout.getChildren().add(headerCard);

        // Strategies Flow Grid
        Label stLabel = new Label("Actionable Study Techniques Designed For You");
        stLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 10 0 0 0;");
        layout.getChildren().add(stLabel);

        javafx.scene.layout.FlowPane stratGrid = new javafx.scene.layout.FlowPane(16, 16);
        List<tracker.model.StudyStrategy> strategies = profileService.getStudentStrategies(currentStudentDbId);

        String[] brightColors = { "#ff6b81", "#7bed9f", "#70a1ff", "#eccc68", "#ff7f50" };
        int colorIdx = 0;

        for (tracker.model.StudyStrategy st : strategies) {
            VBox scard = new VBox(12);
            scard.getStyleClass().add("card");
            scard.setPrefWidth(350);

            Label stName = new Label("⚡ " + st.getStrategyName());
            stName.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: "
                    + brightColors[colorIdx % brightColors.length] + ";");

            Label stDesc = new Label(st.getInstructions());
            stDesc.setStyle("-fx-font-size: 14px; -fx-text-fill: #ced6e0;");
            stDesc.setWrapText(true);

            scard.getChildren().addAll(stName, stDesc);
            stratGrid.getChildren().add(scard);
            colorIdx++;
        }

        layout.getChildren().add(stratGrid);
        return layout;
    }
}
