package tracker.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based recommendation engine that maps weak subjects to
 * prerequisite topics a student should review.
 *
 * Uses hardcoded mappings -- no external data source required.
 */
public class RecommendationEngine {

        private final Map<String, List<String>> recommendationMap;

        public RecommendationEngine() {
                recommendationMap = new HashMap<>();
                initializeRecommendations();
        }

        /**
         * Populates the recommendation map with subject-to-prerequisite mappings.
         * Keys are stored in lowercase for case-insensitive matching.
         */
        private void initializeRecommendations() {
                // Mathematics
                recommendationMap.put("algebra", Arrays.asList(
                                "Review Fractions and Decimals",
                                "Practice Linear Equations",
                                "Study Order of Operations (BODMAS)"));
                recommendationMap.put("geometry", Arrays.asList(
                                "Review Angles and Their Properties",
                                "Practice Triangle Theorems",
                                "Study Coordinate Geometry Basics"));
                recommendationMap.put("calculus", Arrays.asList(
                                "Review Limits and Continuity",
                                "Practice Differentiation Rules",
                                "Study Integration Fundamentals"));
                recommendationMap.put("mathematics", Arrays.asList(
                                "Review Arithmetic Fundamentals",
                                "Practice Problem-Solving Strategies",
                                "Study Number Theory Basics"));
                recommendationMap.put("math", Arrays.asList(
                                "Review Arithmetic Fundamentals",
                                "Practice Problem-Solving Strategies",
                                "Study Number Theory Basics"));
                recommendationMap.put("statistics", Arrays.asList(
                                "Review Probability Basics",
                                "Practice Mean, Median, Mode Calculations",
                                "Study Data Representation Techniques"));

                // Science
                recommendationMap.put("physics", Arrays.asList(
                                "Review Basic Mechanics (Newton's Laws)",
                                "Practice Unit Conversion",
                                "Study Energy and Work Concepts"));
                recommendationMap.put("chemistry", Arrays.asList(
                                "Review Periodic Table Basics",
                                "Practice Balancing Chemical Equations",
                                "Study Atomic Structure"));
                recommendationMap.put("biology", Arrays.asList(
                                "Review Cell Structure and Function",
                                "Practice Genetics Fundamentals",
                                "Study Human Anatomy Basics"));
                recommendationMap.put("science", Arrays.asList(
                                "Review Scientific Method",
                                "Practice Lab Report Writing",
                                "Study Basic Experimental Design"));

                // Computing
                recommendationMap.put("programming", Arrays.asList(
                                "Review Variables and Data Types",
                                "Practice Loops and Control Flow",
                                "Study Functions and Methods"));
                recommendationMap.put("computer science", Arrays.asList(
                                "Review Data Structures Basics",
                                "Practice Algorithm Design",
                                "Study Computational Thinking"));
                recommendationMap.put("databases", Arrays.asList(
                                "Review SQL Fundamentals",
                                "Practice ER Diagram Design",
                                "Study Normalization Concepts"));

                // Languages
                recommendationMap.put("english", Arrays.asList(
                                "Review Grammar and Sentence Structure",
                                "Practice Essay Writing Techniques",
                                "Study Comprehension Strategies"));
                recommendationMap.put("literature", Arrays.asList(
                                "Review Literary Devices",
                                "Practice Critical Analysis Writing",
                                "Study Major Literary Movements"));

                // Social Sciences
                recommendationMap.put("history", Arrays.asList(
                                "Review Timeline and Chronology Skills",
                                "Practice Source Analysis",
                                "Study Key Historical Events"));
                recommendationMap.put("economics", Arrays.asList(
                                "Review Supply and Demand Basics",
                                "Practice Graph Interpretation",
                                "Study Market Structures"));
                recommendationMap.put("geography", Arrays.asList(
                                "Review Map Reading Skills",
                                "Practice Climate and Weather Patterns",
                                "Study Physical Geography Basics"));
        }

        /**
         * Generates recommendations for a list of weak subject names.
         * Performs case-insensitive matching against the recommendation map.
         * If a subject has no specific mapping, a generic recommendation is provided.
         *
         * @param weakSubjects list of weak subject names
         * @return list of recommendation strings
         */
        public List<String> getRecommendations(List<String> weakSubjects) {
                List<String> recommendations = new ArrayList<>();

                if (weakSubjects == null || weakSubjects.isEmpty()) {
                        recommendations.add("Great job! No weak subjects detected. Keep up the good work!");
                        return recommendations;
                }

                for (String subject : weakSubjects) {
                        if (subject == null) {
                                continue;
                        }
                        String key = subject.trim().toLowerCase();
                        List<String> mapped = recommendationMap.get(key);

                        if (mapped != null) {
                                recommendations.add("[" + subject + "] Recommended topics:");
                                for (String rec : mapped) {
                                        recommendations.add("   -> " + rec);
                                }
                        } else {
                                // Generic recommendation for unmapped subjects
                                recommendations.add("[" + subject + "] Recommended topics:");
                                recommendations.add("   -> Review foundational concepts in " + subject);
                                recommendations.add("   -> Practice past exam questions for " + subject);
                                recommendations.add("   -> Seek tutoring or additional study materials for " + subject);
                        }
                }

                return recommendations;
        }
}
