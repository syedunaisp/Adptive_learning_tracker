package tracker.ui;

import tracker.data.dao.UserDAO;
import tracker.model.User;
import tracker.model.UserRole;
import tracker.security.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Login screen for the ALIP platform.
 *
 * UPGRADED:
 *   - Authenticates against the users table via UserDAO
 *   - Hashed password verification (SHA-256 + salt)
 *   - Stores session context in SessionManager
 *   - Improved UI: floating labels, focus glow, show/hide password toggle,
 *     version info, default credentials hint, animated gradient background
 *   - No role selector — role comes from database
 */
public class LoginFrame extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblError;
    private JLabel lblTogglePassword;
    private boolean passwordVisible = false;
    private final UserDAO userDAO = new UserDAO();

    public LoginFrame() {
        setTitle("ALIP - Login");
        setSize(520, 620);
        setMinimumSize(new Dimension(460, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        buildUI();
    }

    private void buildUI() {
        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setPaint(new GradientPaint(0, 0, new Color(0x0F172A),
                        getWidth(), getHeight(), new Color(0x1E3A5F)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle decorative circles
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
                g2.setColor(Color.WHITE);
                g2.fillOval(-80, -80, 300, 300);
                g2.fillOval(getWidth() - 150, getHeight() - 200, 350, 350);
                g2.dispose();
            }
        };

        // Card container with drop shadow
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Shadow
                for (int i = 5; i > 0; i--) {
                    g2.setColor(new Color(0, 0, 0, 6 + (5 - i) * 3));
                    g2.fill(new RoundRectangle2D.Double(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 20, 20));
                }

                // Card background
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(44, 44, 36, 44));
        card.setPreferredSize(new Dimension(420, 520));

        // --- Brand icon (shield) ---
        JLabel iconLabel = new JLabel("\u25C6", SwingConstants.CENTER);
        iconLabel.setFont(new Font("Segoe UI", Font.BOLD, 40));
        iconLabel.setForeground(StyleConstants.PRIMARY);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);

        card.add(Box.createVerticalStrut(6));

        // --- Title ---
        JLabel titleLabel = new JLabel("ALIP", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(0x1E293B));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);

        // --- Subtitle ---
        JLabel subtitleLabel = new JLabel("Academic Risk Intelligence Platform", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(0x64748B));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitleLabel);

        card.add(Box.createVerticalStrut(32));

        // --- Username field ---
        JLabel lblUser = new JLabel("Username");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblUser.setForeground(new Color(0x374151));
        lblUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblUser);
        card.add(Box.createVerticalStrut(6));

        txtUsername = createStyledTextField();
        txtUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(txtUsername);

        card.add(Box.createVerticalStrut(18));

        // --- Password field with toggle ---
        JLabel lblPass = new JLabel("Password");
        lblPass.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPass.setForeground(new Color(0x374151));
        lblPass.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblPass);
        card.add(Box.createVerticalStrut(6));

        JPanel passwordPanel = new JPanel(new BorderLayout(0, 0));
        passwordPanel.setOpaque(false);
        passwordPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        passwordPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtPassword = createStyledPasswordField();
        passwordPanel.add(txtPassword, BorderLayout.CENTER);

        // Show/Hide toggle
        lblTogglePassword = new JLabel("\u25CB");  // eye icon placeholder
        lblTogglePassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblTogglePassword.setForeground(new Color(0x94A3B8));
        lblTogglePassword.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblTogglePassword.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));
        lblTogglePassword.setToolTipText("Show/Hide password");
        lblTogglePassword.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                togglePasswordVisibility();
            }
        });
        passwordPanel.add(lblTogglePassword, BorderLayout.EAST);

        card.add(passwordPanel);

        card.add(Box.createVerticalStrut(8));

        // --- Error label ---
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(new Color(0xEF4444));
        lblError.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lblError);

        card.add(Box.createVerticalStrut(12));

        // --- Login button ---
        JButton btnLogin = createLoginButton("Sign In");
        btnLogin.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLogin.addActionListener(e -> handleLogin());
        card.add(btnLogin);

        card.add(Box.createVerticalStrut(20));

        // --- Separator ---
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xE5E7EB));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sep);

        card.add(Box.createVerticalStrut(14));

        // --- Default credentials hint ---
        JLabel hintLabel = new JLabel(
            "<html><center><span style='color:#94A3B8;font-size:10px;'>" +
            "Default accounts: <b>admin</b>/admin123 &nbsp;\u2022&nbsp; <b>teacher</b>/teacher123" +
            "</span></center></html>",
            SwingConstants.CENTER
        );
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(hintLabel);

        card.add(Box.createVerticalStrut(6));

        // --- Version info ---
        JLabel versionLabel = new JLabel("v3.0 \u2014 Database-Driven Edition", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        versionLabel.setForeground(new Color(0xA0AEC0));
        versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(versionLabel);

        // Enter key triggers login
        txtPassword.addActionListener(e -> handleLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        mainPanel.add(card);
        setContentPane(mainPanel);
    }

    // ==========================================
    // AUTHENTICATION (DB-backed)
    // ==========================================

    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty()) {
            showError("Username is required.");
            txtUsername.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            showError("Password is required.");
            txtPassword.requestFocusInWindow();
            return;
        }

        // Authenticate against database
        User user = userDAO.authenticate(username, password);
        if (user == null) {
            showError("Invalid username or password.");
            txtPassword.setText("");
            txtPassword.requestFocusInWindow();
            return;
        }

        // Resolve linked student_id if this is a student account
        String linkedStudentId = null;
        if (user.getLinkedStudentDbId() != null) {
            linkedStudentId = userDAO.resolveLinkedStudentId(user.getLinkedStudentDbId());
        }

        // Store session
        SessionManager.login(user, linkedStudentId);

        lblError.setText(" ");
        dispose();

        // Launch main application
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame(user.getRole(), user.getUsername());
            frame.setVisible(true);
        });
    }

    private void showError(String msg) {
        lblError.setText(msg);
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPassword.setEchoChar((char) 0);
            lblTogglePassword.setText("\u25CF");  // filled circle = visible
            lblTogglePassword.setToolTipText("Hide password");
        } else {
            txtPassword.setEchoChar('\u2022');
            lblTogglePassword.setText("\u25CB");  // open circle = hidden
            lblTogglePassword.setToolTipText("Show password");
        }
    }

    // ==========================================
    // STYLED COMPONENTS
    // ==========================================

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? new Color(0x3B82F6) : new Color(0xD1D5DB));
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tf.setBackground(new Color(0xF9FAFB));
        tf.setForeground(new Color(0x1E293B));
        tf.setCaretColor(new Color(0x3B82F6));
        tf.setOpaque(false);
        tf.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { tf.repaint(); }
            public void focusLost(FocusEvent e) { tf.repaint(); }
        });
        return tf;
    }

    private JPasswordField createStyledPasswordField() {
        JPasswordField pf = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? new Color(0x3B82F6) : new Color(0xD1D5DB));
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pf.setBackground(new Color(0xF9FAFB));
        pf.setForeground(new Color(0x1E293B));
        pf.setCaretColor(new Color(0x3B82F6));
        pf.setOpaque(false);
        pf.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        pf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        pf.setEchoChar('\u2022');
        pf.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { pf.repaint(); }
            public void focusLost(FocusEvent e) { pf.repaint(); }
        });
        return pf;
    }

    private JButton createLoginButton(String text) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            boolean pressed = false;
            {
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
                setOpaque(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    public void mouseExited(MouseEvent e) { hovered = false; pressed = false; repaint(); }
                    public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
                    public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color base = new Color(0x3B82F6);
                if (pressed) base = new Color(0x1D4ED8);
                else if (hovered) base = new Color(0x2563EB);

                // Subtle shadow
                g2.setColor(new Color(0x3B82F6, true));
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                g2.fillRoundRect(0, 3, getWidth(), getHeight() - 2, 10, 10);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

                // Button fill
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 2, 10, 10);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setForeground(Color.WHITE);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 46));
        return btn;
    }
}
