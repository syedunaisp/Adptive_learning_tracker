package tracker.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom styled button with:
 * - Rounded corners
 * - Darken-on-hover effect (not brighten)
 * - Hand cursor
 * - Smooth antialiased rendering
 *
 * UPGRADED: Uses darken effect instead of brighten for professional look.
 */
public class StyledButton extends JButton {

    private boolean isHovered = false;
    private boolean isPressed = false;

    public StyledButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bgColor = getBackground();

        if (isPressed) {
            bgColor = darken(bgColor, 0.75);
        } else if (isHovered) {
            bgColor = darken(bgColor, 0.85);
        }

        // Paint rounded rectangle background
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

        // Paint text/icon
        super.paintComponent(g);

        g2.dispose();
    }

    /**
     * Darkens a color by the given factor (0.0 = black, 1.0 = unchanged).
     */
    private Color darken(Color c, double factor) {
        return new Color(
                Math.max((int) (c.getRed() * factor), 0),
                Math.max((int) (c.getGreen() * factor), 0),
                Math.max((int) (c.getBlue() * factor), 0),
                c.getAlpha()
        );
    }
}
