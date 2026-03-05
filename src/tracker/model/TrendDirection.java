package tracker.model;

/**
 * Represents the direction of a student's academic performance trend.
 * Determined by comparing current average against historical average.
 */
public enum TrendDirection {

    IMPROVING("Improving", "+"),
    STABLE("Stable", "="),
    DECLINING("Declining", "-");

    private final String label;
    private final String symbol;

    TrendDirection(String label, String symbol) {
        this.label = label;
        this.symbol = symbol;
    }

    public String getLabel() {
        return label;
    }

    public String getSymbol() {
        return symbol;
    }

    /**
     * Returns a UI-friendly arrow indicator.
     */
    public String getArrow() {
        switch (this) {
            case IMPROVING: return "^";
            case DECLINING: return "v";
            default:        return "=";
        }
    }

    @Override
    public String toString() {
        return label + " (" + symbol + ")";
    }
}
