package tracker.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Reusable card container with:
 * - White background
 * - Rounded corners (12px radius)
 * - Subtle drop shadow
 * - Internal padding (20-24px)
 * - No harsh borders
 *
 * Used to wrap content sections throughout the dashboard.
 */
public class DashboardCard extends JPanel {

    private String cardTitle;
    private boolean showTitle;

    /**
     * Creates a card with a visible title header.
     */
    public DashboardCard(String title) {
        this(title, true);
    }

    /**
     * Creates a card with optional title visibility.
     */
    public DashboardCard(String title, boolean showTitle) {
        this.cardTitle = title;
        this.showTitle = showTitle;
        setOpaque(false);
        setLayout(new BorderLayout(0, showTitle ? 12 : 0));
        setBorder(BorderFactory.createEmptyBorder(
                StyleConstants.CARD_SHADOW_SIZE + 20,
                StyleConstants.CARD_SHADOW_SIZE + 24,
                StyleConstants.CARD_SHADOW_SIZE + 20,
                StyleConstants.CARD_SHADOW_SIZE + 24));

        if (showTitle && title != null && !title.isEmpty()) {
            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(StyleConstants.SUBHEADER_FONT);
            titleLabel.setForeground(StyleConstants.TEXT_FG);
            add(titleLabel, BorderLayout.NORTH);
        }
    }

    /**
     * Creates a card with no title.
     */
    public DashboardCard() {
        this(null, false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shadowSize = StyleConstants.CARD_SHADOW_SIZE;
        int radius = StyleConstants.CARD_RADIUS;
        int w = getWidth() - shadowSize * 2;
        int h = getHeight() - shadowSize * 2;

        // Draw shadow layers (subtle gradient)
        for (int i = shadowSize; i > 0; i--) {
            int alpha = 8 + (shadowSize - i) * 3;
            g2.setColor(new Color(0, 0, 0, Math.min(alpha, 30)));
            g2.fillRoundRect(shadowSize - i + 1, shadowSize - i + 2,
                    w + i * 2 - 2, h + i * 2 - 2,
                    radius + i, radius + i);
        }

        // Draw card background
        g2.setColor(StyleConstants.CARD_BG);
        g2.fillRoundRect(shadowSize, shadowSize, w, h, radius, radius);

        // Draw subtle border
        g2.setColor(StyleConstants.CARD_BORDER);
        g2.drawRoundRect(shadowSize, shadowSize, w - 1, h - 1, radius, radius);

        g2.dispose();
        super.paintComponent(g);
    }

    /**
     * Adds the given component to the CENTER of this card.
     * Convenience method for wrapping content.
     */
    public void setContent(JComponent content) {
        add(content, BorderLayout.CENTER);
    }

    public String getCardTitle() {
        return cardTitle;
    }
}
