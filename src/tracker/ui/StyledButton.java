package tracker.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A custom button that enforces specific styling (background color, rounded
 * corners)
 * regardless of the underlying Look and Feel (e.g., Windows).
 */
public class StyledButton extends JButton {

    private boolean isHovered = false;

    public StyledButton(String text) {
        super(text);
        setContentAreaFilled(false); // Disable native background painting
        setFocusPainted(false); // Remove focus border
        setBorderPainted(false); // Handle border manually if needed
        setOpaque(false); // Transparent so we can paint a rounded shape
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover listener
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Determine background color
        // If we are hovered, slightly brighten or darken the color
        Color bgColor = getBackground();
        if (isHovered) {
            bgColor = bgColor.brighter();
        }

        // Paint rounded rectangle background
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // Paint text (and icon if any)
        super.paintComponent(g); // This paints the text/icon relative to component

        g2.dispose();
    }

    /**
     * Overridden to ensure text is painted correctly on top of our custom
     * background.
     * We don't need to do much here since super.paintComponent(g) does the text
     * painting,
     * but we rely on the setContentAreaFilled(false) to stop it from painting the
     * native block.
     */
}
