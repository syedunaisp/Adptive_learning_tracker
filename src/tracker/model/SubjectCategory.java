package tracker.model;

import java.util.*;

/**
 * Defines configurable academic subject categories.
 * Each category maps to a set of known subject names (case-insensitive).
 *
 * Administrators can modify the mappings at runtime via
 * {@link #addSubjectToCategory(String, SubjectCategory)}.
 */
public enum SubjectCategory {

    STEM("STEM (Science, Technology, Engineering, Math)"),
    LANGUAGE("Language & Literature"),
    SOCIAL_SCIENCES("Social Sciences"),
    UNCATEGORIZED("Uncategorized");

    private final String displayName;

    /** Shared mutable map: lowercase subject name -> category. */
    private static final Map<String, SubjectCategory> SUBJECT_MAP = new HashMap<>();

    static {
        // --- STEM ---
        for (String s : new String[]{
                "math", "mathematics", "algebra", "geometry", "calculus", "statistics",
                "trigonometry", "physics", "chemistry", "biology", "science",
                "computer science", "programming", "databases", "engineering",
                "ict", "information technology"}) {
            SUBJECT_MAP.put(s, STEM);
        }

        // --- Language ---
        for (String s : new String[]{
                "english", "literature", "arabic", "french", "spanish", "german",
                "urdu", "hindi", "language", "writing", "reading"}) {
            SUBJECT_MAP.put(s, LANGUAGE);
        }

        // --- Social Sciences ---
        for (String s : new String[]{
                "history", "geography", "economics", "sociology", "psychology",
                "political science", "civics", "social studies", "philosophy",
                "business studies", "accounting"}) {
            SUBJECT_MAP.put(s, SOCIAL_SCIENCES);
        }
    }

    SubjectCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Looks up the category for a given subject name (case-insensitive).
     *
     * @param subjectName the subject name
     * @return the matching category, or UNCATEGORIZED if not mapped
     */
    public static SubjectCategory categorize(String subjectName) {
        if (subjectName == null) return UNCATEGORIZED;
        SubjectCategory cat = SUBJECT_MAP.get(subjectName.trim().toLowerCase());
        return cat != null ? cat : UNCATEGORIZED;
    }

    /**
     * Allows administrators to add or reassign a subject to a category.
     *
     * @param subjectName the subject name (stored lowercase)
     * @param category    the target category
     */
    public static void addSubjectToCategory(String subjectName, SubjectCategory category) {
        if (subjectName != null && category != null) {
            SUBJECT_MAP.put(subjectName.trim().toLowerCase(), category);
        }
    }

    /**
     * Returns all subject names currently mapped to the given category.
     */
    public static List<String> getSubjectsInCategory(SubjectCategory category) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, SubjectCategory> entry : SUBJECT_MAP.entrySet()) {
            if (entry.getValue() == category) {
                result.add(entry.getKey());
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Returns an unmodifiable view of the complete subject-to-category map.
     */
    public static Map<String, SubjectCategory> getAllMappings() {
        return Collections.unmodifiableMap(SUBJECT_MAP);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
