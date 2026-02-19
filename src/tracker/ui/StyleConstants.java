package tracker.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;

/**
 * Centralized style constants for the Adaptive Learning Progress Tracker.
 * Defines fonts, colors, padding, and sizing so the UI has a consistent,
 * modern aesthetic without any external libraries.
 *
 * Font family: "Segoe UI" (Windows) with fallback to system default.
 * Monospace: "Consolas" with fallback to "Monospaced".
 */
public final class StyleConstants {

    // ========================
    // FONTS
    // ========================

    /** Header font -- used for section titles and prominent labels. */
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);

    /** Body font -- used for labels, text fields, combo boxes, general text. */
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    /** Monospace font -- used for JTextArea report/detail displays. */
    public static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 14);

    /** Table header font -- bold, slightly larger for column headers. */
    public static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    /** Table body font -- regular text inside table cells. */
    public static final Font TABLE_BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    /** Button font -- bold for clear call-to-action. */
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 14);

    /** Tab font -- used on JTabbedPane tab labels. */
    public static final Font TAB_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    /** Status bar font -- used for the bottom status bar messages. */
    public static final Font STATUS_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    // ========================
    // COLORS
    // ========================

    // ========================
    // COLORS
    // ========================

    /** Primary deep blue -- used for buttons, highlights, headings. */
    public static Color PRIMARY = new Color(0x2b579a);

    /** Soft white/gray background -- used as panel/content background. */
    public static Color BACKGROUND = new Color(0xfdfdfd);

    /** Accent red -- used for "At Risk" status text. */
    public static Color ACCENT_RED = new Color(0xd32f2f);

    /** Accent green -- used for "Normal" status text. */
    public static Color ACCENT_GREEN = new Color(0x388e3c);

    /** Table header background -- light gray band behind column headers. */
    public static Color TABLE_HEADER_BG = new Color(0xf0f0f0);

    /** Alternating table row color -- very subtle tint for even rows. */
    public static Color TABLE_ALT_ROW = new Color(0xf5f7fa);

    /** Default table row color -- white for odd rows. */
    public static Color TABLE_ROW_WHITE = Color.WHITE;

    /** Status bar background -- subtle separator at the bottom. */
    public static Color STATUS_BAR_BG = new Color(0xf4f4f4);

    /** Button foreground (text color on primary-colored buttons). */
    public static Color BUTTON_FG = Color.WHITE;

    /** Text color (black on light, white on dark). */
    public static Color TEXT_FG = Color.BLACK;

    private static boolean isDarkTheme = false;

    /**
     * Toggles between Light and Dark themes.
     * Updates the static color fields accordingly.
     * 
     * @param dark true to enable Dark Mode, false for Light Mode.
     */
    public static void setDarkTheme(boolean dark) {
        isDarkTheme = dark;
        if (dark) {
            // Dark Mode Palette
            PRIMARY = new Color(0x4a90e2); // Lighter blue for better contrast on dark
            BACKGROUND = new Color(0x2d2d2d); // Dark gray background
            ACCENT_RED = new Color(0xff6b6b); // Brighter red
            ACCENT_GREEN = new Color(0x69db7c); // Brighter green
            TABLE_HEADER_BG = new Color(0x3e3e3e);
            TABLE_ALT_ROW = new Color(0x383838);
            TABLE_ROW_WHITE = new Color(0x2d2d2d);
            STATUS_BAR_BG = new Color(0x252525);
            BUTTON_FG = Color.WHITE;
            TEXT_FG = new Color(0xe0e0e0); // Light text
        } else {
            // Light Mode Palette (Original)
            PRIMARY = new Color(0x2b579a);
            BACKGROUND = new Color(0xfdfdfd);
            ACCENT_RED = new Color(0xd32f2f);
            ACCENT_GREEN = new Color(0x388e3c);
            TABLE_HEADER_BG = new Color(0xf0f0f0);
            TABLE_ALT_ROW = new Color(0xf5f7fa);
            TABLE_ROW_WHITE = Color.WHITE;
            STATUS_BAR_BG = new Color(0xf4f4f4);
            BUTTON_FG = Color.WHITE;
            TEXT_FG = Color.BLACK;
        }
    }

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    /**
     * Applies the current theme colors and fonts to the UIManager defaults.
     * This ensures standard components (Panels, Labels, OptionPanes) pick up the
     * theme.
     */
    public static void configureUIManager() {
        // Fonts
        javax.swing.UIManager.put("Label.font", BODY_FONT);
        javax.swing.UIManager.put("Button.font", BUTTON_FONT);
        javax.swing.UIManager.put("TextField.font", BODY_FONT);
        javax.swing.UIManager.put("TextArea.font", MONO_FONT);
        javax.swing.UIManager.put("ComboBox.font", BODY_FONT);
        javax.swing.UIManager.put("Table.font", TABLE_BODY_FONT);
        javax.swing.UIManager.put("TableHeader.font", TABLE_HEADER_FONT);
        javax.swing.UIManager.put("TabbedPane.font", TAB_FONT);
        javax.swing.UIManager.put("OptionPane.messageFont", BODY_FONT);
        javax.swing.UIManager.put("OptionPane.buttonFont", BUTTON_FONT);
        javax.swing.UIManager.put("TitledBorder.font", HEADER_FONT);

        // Colors - Global
        javax.swing.UIManager.put("Panel.background", BACKGROUND);
        javax.swing.UIManager.put("OptionPane.background", BACKGROUND);
        javax.swing.UIManager.put("Label.foreground", TEXT_FG);
        javax.swing.UIManager.put("TitledBorder.titleColor", PRIMARY);

        // Inputs
        javax.swing.UIManager.put("TextField.background", isDarkTheme ? new Color(0x404040) : Color.WHITE);
        javax.swing.UIManager.put("TextField.foreground", TEXT_FG);
        javax.swing.UIManager.put("TextField.caretForeground", TEXT_FG);

        javax.swing.UIManager.put("TextArea.background", isDarkTheme ? new Color(0x404040) : Color.WHITE);
        javax.swing.UIManager.put("TextArea.foreground", TEXT_FG);

        javax.swing.UIManager.put("ComboBox.background", isDarkTheme ? new Color(0x404040) : Color.WHITE);
        javax.swing.UIManager.put("ComboBox.foreground", TEXT_FG);

        // Tables
        javax.swing.UIManager.put("Table.background", TABLE_ROW_WHITE);
        javax.swing.UIManager.put("Table.foreground", TEXT_FG);
        javax.swing.UIManager.put("Table.gridColor", isDarkTheme ? new Color(0x505050) : new Color(230, 230, 230));
        javax.swing.UIManager.put("TableHeader.background", TABLE_HEADER_BG);
        javax.swing.UIManager.put("TableHeader.foreground", TEXT_FG);

        // Tabs
        javax.swing.UIManager.put("TabbedPane.background", BACKGROUND);
        javax.swing.UIManager.put("TabbedPane.foreground", TEXT_FG);
        javax.swing.UIManager.put("TabbedPane.selected", isDarkTheme ? new Color(0x505050) : new Color(0xe0e0e0));
    }

    /** Standard panel padding -- EmptyBorder(15,15,15,15). */
    public static final EmptyBorder PANEL_PADDING = new EmptyBorder(15, 15, 15, 15);

    /** Insets for form fields inside GridBagLayout. */
    public static final Insets FORM_INSETS = new Insets(8, 10, 8, 10);

    /** Increased table row height for breathing room. */
    public static final int TABLE_ROW_HEIGHT = 30;

    /** Standard button preferred size. */
    public static final Dimension BUTTON_SIZE = new Dimension(180, 38);

    /** Wider button preferred size (for longer labels). */
    public static final Dimension BUTTON_SIZE_WIDE = new Dimension(340, 38);

    /** Text field inner padding (top, left, bottom, right). */
    public static final Insets TEXT_FIELD_MARGIN = new Insets(4, 8, 4, 8);

    /** Combo box preferred height. */
    public static final int COMBO_HEIGHT = 32;

    // ========================
    // LAYOUT GAPS
    // ========================

    /** Horizontal gap for BorderLayout panels. */
    public static final int GAP_H = 12;

    /** Vertical gap for BorderLayout panels. */
    public static final int GAP_V = 12;

    /** Private constructor -- this is a constants-only utility class. */
    private StyleConstants() {
        // Prevent instantiation
    }
}
