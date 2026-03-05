package tracker.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;

/**
 * Centralized style constants for the ALIP platform.
 * Defines a modern SaaS-style palette with clean typography,
 * generous whitespace, and soft color accents.
 *
 * UPGRADED: Modern dashboard palette replacing the original theme.
 */
public final class StyleConstants {

    // ========================
    // FONTS
    // ========================

    /** Large title font for dashboard headers. */
    public static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 22);

    /** Header font for section titles. */
    public static final Font HEADER_FONT = new Font("Segoe UI", Font.BOLD, 16);

    /** Sub-header font for card titles and labels. */
    public static final Font SUBHEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    /** Body font for labels, text fields, general text. */
    public static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    /** Small font for secondary info, timestamps. */
    public static final Font SMALL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    /** Monospace font for report/detail text areas. */
    public static final Font MONO_FONT = new Font("Consolas", Font.PLAIN, 13);

    /** Table header font. */
    public static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 13);

    /** Table body font. */
    public static final Font TABLE_BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    /** Button font. */
    public static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 13);

    /** Tab font (kept for any remaining tab usage). */
    public static final Font TAB_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    /** Status bar font. */
    public static final Font STATUS_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    /** Metric card large number font. */
    public static final Font METRIC_NUMBER_FONT = new Font("Segoe UI", Font.BOLD, 32);

    /** Metric card label font. */
    public static final Font METRIC_LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    /** Sidebar item font. */
    public static final Font SIDEBAR_FONT = new Font("Segoe UI", Font.PLAIN, 14);

    /** Sidebar item font when selected. */
    public static final Font SIDEBAR_FONT_SELECTED = new Font("Segoe UI", Font.BOLD, 14);

    /** Header bar title font. */
    public static final Font HEADER_TITLE_FONT = new Font("Segoe UI", Font.BOLD, 18);

    /** Header bar subtitle font. */
    public static final Font HEADER_SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    // ========================
    // COLORS — MODERN SAAS PALETTE
    // ========================

    /** Primary accent blue. */
    public static Color PRIMARY = new Color(0x3B82F6);

    /** Primary hover (darker). */
    public static final Color PRIMARY_HOVER = new Color(0x2563EB);

    /** Primary light (for subtle backgrounds). */
    public static final Color PRIMARY_LIGHT = new Color(0xDBEAFE);

    /** Page background — soft light gray. */
    public static Color BACKGROUND = new Color(0xF1F5F9);

    /** Card background — white. */
    public static final Color CARD_BG = Color.WHITE;

    /** Card subtle shadow color. */
    public static final Color CARD_SHADOW = new Color(0, 0, 0, 25);

    /** Card border color (very subtle). */
    public static final Color CARD_BORDER = new Color(0xE2E8F0);

    /** Sidebar background — dark slate. */
    public static final Color SIDEBAR_BG = new Color(0x1E293B);

    /** Sidebar item text color. */
    public static final Color SIDEBAR_FG = new Color(0x94A3B8);

    /** Sidebar selected item background. */
    public static final Color SIDEBAR_SELECTED_BG = new Color(0x334155);

    /** Sidebar selected item text color. */
    public static final Color SIDEBAR_SELECTED_FG = Color.WHITE;

    /** Sidebar hover background. */
    public static final Color SIDEBAR_HOVER_BG = new Color(0x2D3A4F);

    /** Sidebar brand area background (slightly darker). */
    public static final Color SIDEBAR_BRAND_BG = new Color(0x0F172A);

    /** Header bar background. */
    public static final Color HEADER_BG = Color.WHITE;

    /** Header bar bottom border. */
    public static final Color HEADER_BORDER = new Color(0xE2E8F0);

    /** Accent red — for high risk. */
    public static Color ACCENT_RED = new Color(0xEF4444);

    /** Soft red background for badges. */
    public static final Color ACCENT_RED_BG = new Color(0xFEE2E2);

    /** Accent orange — for moderate risk. */
    public static final Color ACCENT_ORANGE = new Color(0xF97316);

    /** Soft orange background for badges. */
    public static final Color ACCENT_ORANGE_BG = new Color(0xFFF7ED);

    /** Accent green — for low risk / normal. */
    public static Color ACCENT_GREEN = new Color(0x22C55E);

    /** Soft green background for badges. */
    public static final Color ACCENT_GREEN_BG = new Color(0xDCFCE7);

    /** Accent blue for stable/info. */
    public static final Color ACCENT_BLUE = new Color(0x3B82F6);

    /** Soft blue background. */
    public static final Color ACCENT_BLUE_BG = new Color(0xDBEAFE);

    /** Table header background. */
    public static Color TABLE_HEADER_BG = new Color(0xF8FAFC);

    /** Alternating table row color. */
    public static Color TABLE_ALT_ROW = new Color(0xF8FAFC);

    /** Default table row color. */
    public static Color TABLE_ROW_WHITE = Color.WHITE;

    /** Status bar background. */
    public static Color STATUS_BAR_BG = new Color(0xf4f4f4);

    /** Button foreground (text on primary buttons). */
    public static Color BUTTON_FG = Color.WHITE;

    /** Text primary — dark. */
    public static Color TEXT_FG = new Color(0x1E293B);

    /** Text secondary — muted. */
    public static final Color TEXT_SECONDARY = new Color(0x64748B);

    /** Text tertiary — very muted. */
    public static final Color TEXT_TERTIARY = new Color(0x94A3B8);

    /** Divider / separator line color. */
    public static final Color DIVIDER = new Color(0xE2E8F0);

    /** Input field border. */
    public static final Color INPUT_BORDER = new Color(0xCBD5E1);

    /** Input field focus border. */
    public static final Color INPUT_FOCUS = new Color(0x3B82F6);

    /** Error red for validation. */
    public static final Color ERROR_RED = new Color(0xEF4444);

    // Dark theme support (preserved)
    private static boolean isDarkTheme = false;

    public static void setDarkTheme(boolean dark) {
        isDarkTheme = dark;
        // Dark theme adjustments could be added here
    }

    public static boolean isDarkTheme() {
        return isDarkTheme;
    }

    /**
     * Applies current theme to UIManager defaults.
     */
    public static void configureUIManager() {
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
        javax.swing.UIManager.put("TitledBorder.font", SUBHEADER_FONT);

        javax.swing.UIManager.put("Panel.background", BACKGROUND);
        javax.swing.UIManager.put("OptionPane.background", CARD_BG);
        javax.swing.UIManager.put("Label.foreground", TEXT_FG);
        javax.swing.UIManager.put("TitledBorder.titleColor", TEXT_FG);

        javax.swing.UIManager.put("TextField.background", Color.WHITE);
        javax.swing.UIManager.put("TextField.foreground", TEXT_FG);
        javax.swing.UIManager.put("TextField.caretForeground", TEXT_FG);

        javax.swing.UIManager.put("TextArea.background", Color.WHITE);
        javax.swing.UIManager.put("TextArea.foreground", TEXT_FG);

        javax.swing.UIManager.put("ComboBox.background", Color.WHITE);
        javax.swing.UIManager.put("ComboBox.foreground", TEXT_FG);

        javax.swing.UIManager.put("Table.background", TABLE_ROW_WHITE);
        javax.swing.UIManager.put("Table.foreground", TEXT_FG);
        javax.swing.UIManager.put("Table.gridColor", DIVIDER);
        javax.swing.UIManager.put("TableHeader.background", TABLE_HEADER_BG);
        javax.swing.UIManager.put("TableHeader.foreground", TEXT_FG);

        javax.swing.UIManager.put("ScrollPane.background", BACKGROUND);
        javax.swing.UIManager.put("Viewport.background", CARD_BG);
    }

    // ========================
    // SPACING & SIZING
    // ========================

    /** Standard panel padding. */
    public static final EmptyBorder PANEL_PADDING = new EmptyBorder(20, 24, 20, 24);

    /** Card internal padding. */
    public static final EmptyBorder CARD_PADDING = new EmptyBorder(20, 24, 20, 24);

    /** Compact card padding. */
    public static final EmptyBorder CARD_PADDING_COMPACT = new EmptyBorder(16, 20, 16, 20);

    /** Insets for form fields. */
    public static final Insets FORM_INSETS = new Insets(6, 10, 6, 10);

    /** Table row height — modern spacious. */
    public static final int TABLE_ROW_HEIGHT = 40;

    /** Standard button size. */
    public static final Dimension BUTTON_SIZE = new Dimension(160, 38);

    /** Wide button size. */
    public static final Dimension BUTTON_SIZE_WIDE = new Dimension(240, 38);

    /** Text field inner padding. */
    public static final Insets TEXT_FIELD_MARGIN = new Insets(6, 10, 6, 10);

    /** Combo box height. */
    public static final int COMBO_HEIGHT = 36;

    /** Sidebar width. */
    public static final int SIDEBAR_WIDTH = 220;

    /** Header bar height. */
    public static final int HEADER_HEIGHT = 60;

    /** Horizontal gap. */
    public static final int GAP_H = 16;

    /** Vertical gap. */
    public static final int GAP_V = 16;

    /** Card corner radius. */
    public static final int CARD_RADIUS = 12;

    /** Card shadow offset. */
    public static final int CARD_SHADOW_SIZE = 4;

    private StyleConstants() {
        // Prevent instantiation
    }
}
