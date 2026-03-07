package tracker.ui.fx;

/**
 * JavaFX inline CSS style constants for the ALIP platform.
 * Mirrors the design system from Swing StyleConstants.
 */
public final class FxStyles {

    // ---- Color palette ----
    public static final String C_PRIMARY = "#3B82F6";
    public static final String C_PRIMARY_DARK = "#2563EB";
    public static final String C_PRIMARY_LIGHT = "#DBEAFE";

    public static final String C_BACKGROUND = "#F1F5F9";
    public static final String C_CARD_BG = "#FFFFFF";

    public static final String C_SIDEBAR_BG = "#1E293B";
    public static final String C_SIDEBAR_BRAND = "#0F172A";
    public static final String C_SIDEBAR_FG = "#94A3B8";
    public static final String C_SIDEBAR_SEL = "#334155";
    public static final String C_SIDEBAR_HOVER = "#2D3A4F";

    public static final String C_HEADER_BG = "#FFFFFF";
    public static final String C_TEXT_DARK = "#1E293B";
    public static final String C_TEXT_MUTED = "#64748B";

    public static final String C_RED = "#EF4444";
    public static final String C_RED_BG = "#FEE2E2";
    public static final String C_ORANGE = "#F97316";
    public static final String C_ORANGE_BG = "#FFF7ED";
    public static final String C_GREEN = "#22C55E";
    public static final String C_GREEN_BG = "#DCFCE7";
    public static final String C_BLUE = "#3B82F6";
    public static final String C_BLUE_BG = "#DBEAFE";

    public static final String C_BORDER = "#E2E8F0";

    // ---- Combined style strings ----
    public static final String CARD_STYLE = "-fx-background-color: white;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2);" +
            "-fx-padding: 18 22 18 22;";

    public static final String INNER_CARD_STYLE = "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-padding: 14 18 14 18;";

    public static final String PAGE_BG = "-fx-background-color: " + C_BACKGROUND + ";";

    public static final String HEADER_STYLE = "-fx-background-color: white;" +
            "-fx-border-color: " + C_BORDER + ";" +
            "-fx-border-width: 0 0 1 0;" +
            "-fx-padding: 0 24 0 24;";

    public static final String SIDEBAR_STYLE = "-fx-background-color: " + C_SIDEBAR_BG + ";";

    public static final String SIDEBAR_BRAND_STYLE = "-fx-background-color: " + C_SIDEBAR_BRAND + ";" +
            "-fx-padding: 20 20 20 20;";

    public static String primaryButton() {
        return "-fx-background-color: " + C_PRIMARY + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 18 8 18;";
    }

    public static String dangerButton() {
        return "-fx-background-color: " + C_RED + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 18 8 18;";
    }

    public static String coloredButton(String hex) {
        return "-fx-background-color: " + hex + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 8 18 8 18;";
    }

    public static String textField() {
        return "-fx-background-color: #F9FAFB;" +
                "-fx-border-color: #D1D5DB;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13;" +
                "-fx-text-fill: #1E293B;" +
                "-fx-padding: 8 12 8 12;";
    }

    public static String label() {
        return "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13;" +
                "-fx-text-fill: " + C_TEXT_DARK + ";";
    }

    public static String title() {
        return "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 22;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + C_TEXT_DARK + ";";
    }

    public static String sectionTitle() {
        return "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 14;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + C_TEXT_DARK + ";";
    }

    public static String smallLabel() {
        return "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 12;" +
                "-fx-text-fill: " + C_TEXT_MUTED + ";";
    }

    public static String metricNumber(String color) {
        return "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 30;" +
                "-fx-font-weight: bold;" +
                "-fx-text-fill: " + color + ";";
    }

    public static String sidebarItem(boolean selected) {
        return "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 14;" +
                "-fx-text-fill: " + (selected ? "white" : C_SIDEBAR_FG) + ";" +
                "-fx-background-color: " + (selected ? C_SIDEBAR_SEL : "transparent") + ";" +
                "-fx-padding: 12 20 12 20;" +
                "-fx-font-weight: " + (selected ? "bold" : "normal") + ";" +
                "-fx-cursor: hand;";
    }

    public static String badge(String fg, String bg) {
        return "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: " + fg + ";" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 3 8 3 8;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: bold;";
    }

    // Chart data series colors for CSS application
    public static final String CHART_RED = "EF4444";
    public static final String CHART_ORANGE = "F97316";
    public static final String CHART_GREEN = "22C55E";
    public static final String CHART_BLUE = "3B82F6";
    public static final String CHART_SLATE = "64748B";

    private FxStyles() {
    }
}
