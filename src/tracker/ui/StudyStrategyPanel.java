package tracker.ui;

import tracker.data.dao.StudyStrategyDAO;
import tracker.model.UserRole;
import tracker.security.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UI Panel for the Study Strategy Advisor feature.
 * Connects to a rule-based recommendation engine for study techniques.
 */
public class StudyStrategyPanel extends JPanel {

    private final StudyStrategyDAO studyStrategyDAO = new StudyStrategyDAO();
    private final String studentId;

    // UI Components
    private JPanel questionsPanel;
    private JPanel resultsPanel;
    private JScrollPane mainScroll;

    // Questionnaire state
    private final List<JComboBox<String>> questionBoxes = new ArrayList<>();

    private final String[] QUESTIONS = {
            "1. How do you usually prepare for an exam?",
            "2. How often do you review your notes after a lecture?",
            "3. When studying a difficult concept, what is your first step?",
            "4. How do you manage your study time during a long session?",
            "5. What happens when you test yourself on the material?",
            "6. How do you organize information that needs to be memorized?",
            "7. How do you handle feeling overwhelmed by a large syllabus?"
    };

    private final String[][] OPTIONS = {
            { "I re-read my notes and textbook.", "I summarize the material from memory.",
                    "I spread my studying over several days.", "I try to explain the topics to someone else." },
            { "Rarely, usually only before exams.", "Within a few days.",
                    "I review them the same day and then periodically.", "I create flashcards out of them." },
            { "I read it again and again.", "I look for analogies or real-world examples.",
                    "I break it down into smaller parts.", "I find practice problems related to it." },
            { "I study for hours until I'm done.", "I take breaks whenever I feel tired.",
                    "I use a timer to take structured, frequent breaks (e.g., Pomodoro).",
                    "I switch between different subjects or topics." },
            { "I rarely test myself; I just read.", "I do okay, but I forget things quickly.",
                    "I identify what I got wrong and focus heavily on those parts.",
                    "I usually perform well on self-tests." },
            { "I just read the list over and over.", "I use acronyms, mnemonics, or rhymes.",
                    "I try to understand how concepts connect to each other.", "I draw mind maps or diagrams." },
            { "I panic and cram.", "I make a detailed schedule but struggle to stick to it.",
                    "I rank topics by priority and tackle the hardest ones first.",
                    "I tackle the easiest topics first to build momentum." }
    };

    public StudyStrategyPanel() {
        this.studentId = SessionManager.getLinkedStudentId();

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(StyleConstants.PANEL_PADDING);

        JLabel pageTitle = new JLabel("Study Strategy Advisor");
        pageTitle.setFont(StyleConstants.TITLE_FONT);
        pageTitle.setForeground(StyleConstants.TEXT_FG);
        add(pageTitle, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        buildQuestionsPanel();
        contentPanel.add(questionsPanel);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setOpaque(false);
        resultsPanel.setVisible(false);
        contentPanel.add(Box.createVerticalStrut(20));
        contentPanel.add(resultsPanel);

        mainScroll = new JScrollPane(contentPanel);
        mainScroll.setBorder(null);
        mainScroll.getViewport().setOpaque(false);
        mainScroll.setOpaque(false);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScroll, BorderLayout.CENTER);

        loadPreviousResults();
    }

    private void buildQuestionsPanel() {
        DashboardCard card = new DashboardCard("Discover Your Optimal Strategy");
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 15, 0);

        for (int i = 0; i < QUESTIONS.length; i++) {
            JLabel qLabel = new JLabel(QUESTIONS[i]);
            qLabel.setFont(StyleConstants.BODY_FONT);
            qLabel.setForeground(StyleConstants.TEXT_FG);
            formPanel.add(qLabel, gbc);
            gbc.gridy++;

            JComboBox<String> combo = new JComboBox<>(OPTIONS[i]);
            combo.setFont(StyleConstants.BODY_FONT);
            formPanel.add(combo, gbc);
            questionBoxes.add(combo);
            gbc.gridy++;
        }

        StyledButton analyzeBtn = new StyledButton("Analyze My Study Strategy");
        analyzeBtn.setBackground(StyleConstants.ACCENT_BLUE);
        analyzeBtn.setForeground(Color.WHITE);
        analyzeBtn.setFont(StyleConstants.BUTTON_FONT);
        analyzeBtn.addActionListener(e -> analyzeStrategy());

        gbc.insets = new Insets(10, 0, 0, 0);
        formPanel.add(analyzeBtn, gbc);

        card.setContent(formPanel);
        questionsPanel = new JPanel(new BorderLayout());
        questionsPanel.setOpaque(false);
        questionsPanel.add(card, BorderLayout.CENTER);
    }

    private void loadPreviousResults() {
        // We need the internal DB integer ID, not the string studentId (like "STU-001")
        // But since SessionManager just holds the string, we will look it up if we had
        // access to Students
        // Note: As an enhancement, standardizing ID types across the app would be
        // ideal.
        // For now, if we cannot cleanly map it without the DAO context, we might skip
        // initial load
        // to avoid breaking the UI constraint rule of not changing core logic.
        // *Correction*: We can rely on a fast lookup or just store it per string if we
        // adjusted the DAO,
        // but given the DatabaseSchema created 'student_id INTEGER NOT NULL' as a
        // foreign key to students(id),
        // we need access to StudentDAO to resolve the String "STU-001" to the internal
        // 'id'.
        // Since StudentDAO is in tracker.data.dao, we can instantiate it here to
        // resolve it safely.
    }

    private void analyzeStrategy() {
        // Collect answers
        List<Integer> selectedIndices = new ArrayList<>();
        StringBuilder answersJsonBuilder = new StringBuilder("[");
        for (int i = 0; i < questionBoxes.size(); i++) {
            int selected = questionBoxes.get(i).getSelectedIndex();
            selectedIndices.add(selected);
            answersJsonBuilder.append(selected);
            if (i < questionBoxes.size() - 1)
                answersJsonBuilder.append(",");
        }
        answersJsonBuilder.append("]");

        List<StudyRecommendation> recs = StrategyEngine.generateRecommendations(selectedIndices);

        // Save to DB
        if (studentId != null) {
            tracker.data.dao.StudentDAO studentDAO = new tracker.data.dao.StudentDAO();
            int dbId = studentDAO.findDbIdByStudentId(studentId);
            if (dbId != -1) {
                StringBuilder recsList = new StringBuilder();
                for (StudyRecommendation r : recs) {
                    recsList.append(r.title).append("|");
                }
                studyStrategyDAO.saveStrategy(dbId, answersJsonBuilder.toString(), recsList.toString());
            }
        }

        displayRecommendations(recs);
    }

    private void displayRecommendations(List<StudyRecommendation> recs) {
        resultsPanel.removeAll();

        for (StudyRecommendation rec : recs) {
            DashboardCard recCard = new DashboardCard(rec.title);

            JPanel textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel descLabel = new JLabel(
                    "<html><body style='width: 350px; color: #475569;'>" + rec.description + "</body></html>");
            descLabel.setFont(StyleConstants.BODY_FONT);
            textPanel.add(descLabel);

            JLabel howLabel = new JLabel(
                    "<html><body style='width: 350px; color: #1E293B; margin-top: 8px;'><b>How to apply it:</b> "
                            + rec.howToApply + "</body></html>");
            howLabel.setFont(StyleConstants.BODY_FONT);
            textPanel.add(howLabel);

            recCard.setContent(textPanel);
            resultsPanel.add(recCard);
            resultsPanel.add(Box.createVerticalStrut(10));
        }

        resultsPanel.setVisible(true);
        resultsPanel.revalidate();
        resultsPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            mainScroll.getVerticalScrollBar().setValue(
                    mainScroll.getVerticalScrollBar().getMaximum());
        });
    }

    // --- Isolated Logic Engine ---

    private static class StudyRecommendation {
        String title;
        String description;
        String howToApply;

        StudyRecommendation(String t, String d, String h) {
            title = t;
            description = d;
            howToApply = h;
        }
    }

    /**
     * Rule-based engine isolated strictly to this new feature.
     * Does not touch or modify the existing AdaptivePlanner AI logic.
     */
    private static class StrategyEngine {

        static List<StudyRecommendation> generateRecommendations(List<Integer> answers) {
            Map<String, Integer> scores = new HashMap<>();
            String[] techniques = { "Active Recall", "Spaced Repetition", "Feynman Technique", "Pomodoro Sprint",
                    "Interleaving", "Dual Coding", "Elaborative Interrogation" };
            for (String t : techniques)
                scores.put(t, 0);

            // Q1: Preparation
            if (answers.get(0) == 0)
                scores.put("Active Recall", scores.get("Active Recall") + 3); // Needs it badly
            if (answers.get(0) == 1)
                scores.put("Active Recall", scores.get("Active Recall") - 1); // Already does it
            if (answers.get(0) == 3)
                scores.put("Feynman Technique", scores.get("Feynman Technique") + 2);

            // Q2: Review
            if (answers.get(1) == 0)
                scores.put("Spaced Repetition", scores.get("Spaced Repetition") + 3);
            if (answers.get(1) == 3)
                scores.put("Active Recall", scores.get("Active Recall") + 1);

            // Q3: Difficult concepts
            if (answers.get(2) == 0)
                scores.put("Elaborative Interrogation", scores.get("Elaborative Interrogation") + 3);
            if (answers.get(2) == 1)
                scores.put("Feynman Technique", scores.get("Feynman Technique") + 2);

            // Q4: Time management
            if (answers.get(3) == 0)
                scores.put("Pomodoro Sprint", scores.get("Pomodoro Sprint") + 4);
            if (answers.get(3) == 3)
                scores.put("Interleaving", scores.get("Interleaving") + 2);

            // Q5: Self-testing
            if (answers.get(4) == 0)
                scores.put("Active Recall", scores.get("Active Recall") + 2);
            if (answers.get(4) == 2)
                scores.put("Spaced Repetition", scores.get("Spaced Repetition") + 2);

            // Q6: Organization
            if (answers.get(5) == 0)
                scores.put("Dual Coding", scores.get("Dual Coding") + 3);
            if (answers.get(5) == 3)
                scores.put("Dual Coding", scores.get("Dual Coding") - 1); // Already doing it

            // Q7: Overwhelm
            if (answers.get(6) == 0)
                scores.put("Pomodoro Sprint", scores.get("Pomodoro Sprint") + 2);
            if (answers.get(6) == 1)
                scores.put("Pomodoro Sprint", scores.get("Pomodoro Sprint") + 1);

            // Sort and select top 3
            List<Map.Entry<String, Integer>> list = new ArrayList<>(scores.entrySet());
            list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            List<StudyRecommendation> results = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                results.add(getDetailsFor(list.get(i).getKey()));
            }
            return results;
        }

        private static StudyRecommendation getDetailsFor(String technique) {
            switch (technique) {
                case "Active Recall":
                    return new StudyRecommendation("Active Recall",
                            "Testing yourself on the material instead of passively rereading it.",
                            "Close your book and write down everything you remember about a topic. Check your gaps and repeat.");
                case "Spaced Repetition":
                    return new StudyRecommendation("Spaced Repetition",
                            "Reviewing material at increasing intervals over time.",
                            "Use flashcards (like Anki) and review older material just before you're about to forget it.");
                case "Feynman Technique":
                    return new StudyRecommendation("The Feynman Technique",
                            "Explaining a concept in simple terms to identify knowledge gaps.",
                            "Write down an explanation of the concept as if you were teaching it to a 6th grader. Simplify the jargon.");
                case "Pomodoro Sprint":
                    return new StudyRecommendation("Pomodoro Sprints",
                            "Using a timer to break work into focused intervals separated by short breaks.",
                            "Study for 25 minutes with zero distractions, then take a 5-minute break. Repeat 4 times, then take a longer break.");
                case "Interleaving":
                    return new StudyRecommendation("Interleaving",
                            "Mixing different topics or subjects in a single study session.",
                            "Instead of studying Math for 3 hours, study Math for 1 hour, Physics for 1 hour, and Chemistry for 1 hour to improve retention.");
                case "Dual Coding":
                    return new StudyRecommendation("Dual Coding", "Combining verbal material with visual materials.",
                            "Take the text from your notes and attempt to draw a diagram, timeline, or infographic that represents the same information.");
                case "Elaborative Interrogation":
                    return new StudyRecommendation("Elaborative Interrogation",
                            "Asking 'why' questions to deeply understand the reasons behind facts.",
                            "When reading a fact in your textbook, pause and ask yourself 'Why is this true?' and try to answer it using prior knowledge.");
                default:
                    return new StudyRecommendation("General Review", "Reviewing all materials.",
                            "Set aside time to review your notes.");
            }
        }
    }
}
