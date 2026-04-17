package com.atm.ui;

import com.atm.model.Account;
import com.atm.service.AuthService;
import com.atm.ui.components.AnimatedButton;
import com.atm.util.AppLogger;
import com.atm.util.LanguageManager;
import com.atm.util.BankThemeManager;
import com.atm.util.BankThemeManager.BankTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LoginFrame - The entry screen of the ATM application.
 * Redesigned to feature a modern, professional Two-Column Layout.
 * - Left side: Dynamic branding graphic with slopes and gradients.
 * - Right side: Minimalist login authentication form.
 */
public class LoginFrame extends JFrame {

    // ── Legacy Static Colors (kept for compatibility) ──
    public static Color BG       = new Color(8,  8,  20);
    public static Color PRIMARY  = new Color(108, 99, 255);
    public static Color ACCENT   = new Color(0,  212, 170);
    public static Color TXT      = new Color(224, 224, 240);
    public static Color TXT_DIM  = new Color(120, 120, 160);
    public static Color ERR      = new Color(255, 107, 107);
    public static Color INPUT_BG = new Color(28,  28,  52);
    public static Color BORDER   = new Color(50,  50,  85);

    // ── Instance Theme Fields ──
    private Color bg, cardBg, primary, accent, txt, txtDim, err, inputBg, border;
    private BufferedImage bankLogoImage;

    // ── Components ──
    private JTextField    custField;
    private JPasswordField pinField;
    private JLabel        statusLabel;
    private AnimatedButton loginBtn;
    
    // Selectors
    private JComboBox<String> langCombo;
    private JComboBox<BankTheme> bankCombo;
    
    // Labels
    private JLabel titleLabel, subLabel, custLabel, pinLabel, langLabel, bankLabel;
    
    // Top-level containers
    private BrandPanel leftPanel;
    private JPanel rightPanel;

    private final LanguageManager langMgr = LanguageManager.getInstance();
    private final BankThemeManager themeMgr = BankThemeManager.getInstance();

    private final AuthService   authService = new AuthService();
    private final ExecutorService executor   = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ATM-Auth");
        t.setDaemon(true);
        return t;
    });

    // ── Constructor ──
    public LoginFrame() {
        // Initialize err and border universally
        err = new Color(255, 107, 107);
        border = new Color(200, 200, 220); // standard border, will be updated via theme

        configFrame();
        buildUI();
        applyBankTheme(themeMgr.getCurrentTheme()); // Apply dynamically
    }

    private void configFrame() {
        setTitle(langMgr.getString("login.window.title"));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Wider layout for two-columns
        setSize(850, 560);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setResizable(false);
    }

    // ── UI Architecture ──
    private void buildUI() {
        setLayout(new GridLayout(1, 2)); // 50/50 split

        leftPanel = new BrandPanel();
        rightPanel = buildLoginPanel();

        add(leftPanel);
        add(rightPanel);
        
        getRootPane().setDefaultButton(loginBtn);
    }

    // ── Right Side: Authentication Panel ──
    private JPanel buildLoginPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(true);

        // 1. Top Control Bar (Language & Bank)
        JPanel topControls = new JPanel(new GridLayout(2, 1, 0, 8));
        topControls.setOpaque(false);
        topControls.setBorder(new EmptyBorder(25, 30, 10, 30));

        JPanel langRow = new JPanel(new BorderLayout());
        langRow.setOpaque(false);
        langLabel = new JLabel(langMgr.getString("login.language.select"));
        langLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        String[] langs = {"English", "हिन्दी (Hindi)"};
        langCombo = new JComboBox<>(langs);
        langCombo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        langCombo.setBorder(BorderFactory.createEmptyBorder());
        langCombo.setFocusable(false);
        langCombo.addActionListener(e -> {
            String selected = (String) langCombo.getSelectedItem();
            langMgr.setLanguage(selected.startsWith("English") ? "en" : "hi");
            updateTexts();
        });
        langRow.add(langLabel, BorderLayout.WEST);
        langRow.add(langCombo, BorderLayout.EAST);

        JPanel bankRow = new JPanel(new BorderLayout());
        bankRow.setOpaque(false);
        bankLabel = new JLabel(langMgr.getString("login.bank.select"));
        bankLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        bankCombo = new JComboBox<>(themeMgr.getThemes().toArray(new BankTheme[0]));
        bankCombo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        bankCombo.setBorder(BorderFactory.createEmptyBorder());
        bankCombo.setFocusable(false);
        bankCombo.addActionListener(e -> {
            BankTheme selected = (BankTheme) bankCombo.getSelectedItem();
            applyBankTheme(selected);
        });
        bankRow.add(bankLabel, BorderLayout.WEST);
        bankRow.add(bankCombo, BorderLayout.EAST);

        topControls.add(langRow);
        topControls.add(bankRow);
        
        container.add(topControls, BorderLayout.NORTH);

        // 2. Central Login Form
        JPanel formCenter = new JPanel(new GridBagLayout());
        formCenter.setOpaque(false);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(300, 380));

        titleLabel = new JLabel("Secure ATM", SwingConstants.LEFT); // updated via applyTheme
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        subLabel = new JLabel(langMgr.getString("login.subheader"), SwingConstants.LEFT);
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setAlignmentX(LEFT_ALIGNMENT);

        custLabel = new JLabel(langMgr.getString("login.label.customer"));
        custLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        custLabel.setAlignmentX(LEFT_ALIGNMENT);
        
        custField = styledTextField();

        pinLabel = new JLabel(langMgr.getString("login.label.pin"));
        pinLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pinLabel.setAlignmentX(LEFT_ALIGNMENT);
        
        pinField = styledPinField();

        loginBtn = new AnimatedButton(langMgr.getString("login.button.login"), Color.BLUE, Color.BLUE.darker());
        loginBtn.setAlignmentX(LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        loginBtn.addActionListener(e -> handleLogin());

        statusLabel = new JLabel(" ", SwingConstants.LEFT);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Assembly
        form.add(Box.createVerticalStrut(30));
        form.add(titleLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(subLabel);
        form.add(Box.createVerticalStrut(36));
        form.add(custLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(custField);
        form.add(Box.createVerticalStrut(20));
        form.add(pinLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(pinField);
        form.add(Box.createVerticalStrut(32));
        form.add(loginBtn);
        form.add(Box.createVerticalStrut(12));
        form.add(statusLabel);

        formCenter.add(form);
        container.add(formCenter, BorderLayout.CENTER);

        return container;
    }

    // ── Custom Left Panel (Dynamic Branding) ──
    private class BrandPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (primary == null || accent == null) return;
            
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background Gradient
            LinearGradientPaint gp = new LinearGradientPaint(
                0, 0, 0, h,
                new float[]{0f, 1f},
                new Color[]{primary.brighter(), primary.darker()}
            );
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);

            // Abstract overlay graphics (Modern Banking Curves)
            g2.setColor(new Color(255, 255, 255, 15)); // slight white alpha
            Path2D path = new Path2D.Float();
            path.moveTo(w * 0.2, h);
            path.curveTo(w * 0.4, h * 0.6, w * 0.8, h * 0.8, w, h * 0.4);
            path.lineTo(w, h);
            path.closePath();
            g2.fill(path);

            Path2D path2 = new Path2D.Float();
            path2.moveTo(0, h * 0.8);
            path2.curveTo(w * 0.3, h * 0.5, w * 0.6, h * 0.9, w * 1.2, h * 0.1);
            path2.lineTo(w, h);
            path2.lineTo(0, h);
            path2.closePath();
            g2.fill(path2);

            // Logo & Texts
            BankTheme currentTheme = themeMgr.getCurrentTheme();
            
            // Draw Logo watermark with alpha transparency for "color grading" blend
            if (bankLogoImage != null) {
                int imgW = 220;
                int imgH = (imgW * bankLogoImage.getHeight()) / bankLogoImage.getWidth();
                int imgX = (w - imgW) / 2;
                int imgY = (h - imgH) / 2 - 30;

                Composite originalComposite = g2.getComposite();
                // 15% opacity to blend professionally into the gradient as a watermark
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                
                g2.drawImage(bankLogoImage, imgX, imgY, imgW, imgH, null);
                
                // Restore original opacity
                g2.setComposite(originalComposite);
            } else {
                // Fallback Emoji Logo
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 80));
                FontMetrics fmEmoji = g2.getFontMetrics();
                int logoWidth = fmEmoji.stringWidth(currentTheme.logo);
                g2.drawString(currentTheme.logo, (w - logoWidth) / 2, h / 2 - 40);
            }

            // Draw Slogan
            g2.setFont(new Font("Segoe UI", Font.BOLD | Font.ITALIC, 22));
            FontMetrics fmSlogan = g2.getFontMetrics();
            String slogan = currentTheme.slogan;
            int sloganW = fmSlogan.stringWidth(slogan);
            // Draw drop shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.drawString(slogan, (w - sloganW) / 2 + 2, (int)(h * 0.75) + 2);
            // Draw text
            g2.setColor(Color.WHITE);
            g2.drawString(slogan, (w - sloganW) / 2, (int)(h * 0.75));

            g2.dispose();
        }
    }

    // ── Theme Management ──
    private void applyBankTheme(BankTheme theme) {
        themeMgr.setCurrentTheme(theme);
        
        // Extract Palette
        this.primary = theme.primary;
        this.accent  = theme.accent;
        this.cardBg  = theme.card;
        this.inputBg = theme.inputBg;
        
        boolean isDark = theme.name.contains("PRO");
        this.bg      = isDark ? new Color(8,  8,  20) : Color.WHITE;
        this.txt     = isDark ? new Color(224, 224, 240) : new Color(30, 30, 45);
        this.txtDim  = isDark ? new Color(120, 120, 160) : new Color(100, 100, 120);
        this.border  = isDark ? new Color(50, 50, 85) : new Color(220, 220, 230);

        // Update Legacy Context
        PRIMARY = this.primary; ACCENT = this.accent;
        BG = this.bg; INPUT_BG = this.inputBg; 
        TXT = this.txt; TXT_DIM = this.txtDim;
        BORDER = this.border;

        // Load Bank Logo Image
        try {
            if (theme.logoPath != null) {
                InputStream is = getClass().getResourceAsStream(theme.logoPath);
                if (is != null) {
                    this.bankLogoImage = ImageIO.read(is);
                } else {
                    AppLogger.info("Logo not found in classpath: " + theme.logoPath);
                    this.bankLogoImage = null;
                }
            }
        } catch (Exception ex) {
            AppLogger.error("Failed to load logo: " + theme.logoPath, ex);
            this.bankLogoImage = null;
        }

        // Apply to Right Panel
        rightPanel.setBackground(bg);
        titleLabel.setForeground(txt);
        subLabel.setForeground(txtDim);
        custLabel.setForeground(txtDim);
        pinLabel.setForeground(txtDim);
        langLabel.setForeground(txtDim);
        bankLabel.setForeground(txtDim);
        statusLabel.setForeground(err);

        // Dropdowns
        langCombo.setBackground(bg);
        langCombo.setForeground(txt);
        bankCombo.setBackground(bg);
        bankCombo.setForeground(txt);
        
        // Inputs
        applyFieldStyle(custField);
        applyFieldStyle(pinField);
        
        // Buttons
        loginBtn.setColors(primary, primary.darker());
        
        // Repaint left panel gradients
        leftPanel.repaint();
        
        updateTexts();
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void updateTexts() {
        BankTheme currentTheme = themeMgr.getCurrentTheme();
        
        setTitle(langMgr.getString("login.window.title"));
        titleLabel.setText(currentTheme.header); // Dynamic Header
        subLabel.setText(langMgr.getString("login.subheader"));
        custLabel.setText(langMgr.getString("login.label.customer"));
        pinLabel.setText(langMgr.getString("login.label.pin"));
        langLabel.setText(langMgr.getString("login.language.select"));
        bankLabel.setText(langMgr.getString("login.bank.select"));
        loginBtn.setText(langMgr.getString("login.button.login"));
        
        String currentCust = custField.getText().trim();
        String oldPlaceholder = custField.getClientProperty("placeholder") != null ? 
                                custField.getClientProperty("placeholder").toString() : "";
        String newPlaceholder = langMgr.getString("login.placeholder.customer");
        
        if (currentCust.isEmpty() || currentCust.equals(oldPlaceholder)) {
            custField.setText(newPlaceholder);
            custField.setForeground(txtDim);
        }
        custField.putClientProperty("placeholder", newPlaceholder);
    }

    // ── Logic ──
    private void handleLogin() {
        String custText    = custField.getText().trim();
        char[] pinChars    = pinField.getPassword();
        String pinText     = new String(pinChars).trim();
        String bankName    = themeMgr.getCurrentTheme().name;
        String placeholder = (String) custField.getClientProperty("placeholder");

        if (custText.isEmpty() || custText.equals(placeholder)) {
            showStatus(langMgr.getString("login.error.customer.empty"), err);
            return;
        }
        if (pinText.isEmpty()) {
            showStatus(langMgr.getString("login.error.pin.empty"), err);
            return;
        }

        int custNo, pin;
        try {
            custNo = Integer.parseInt(custText);
            pin    = Integer.parseInt(pinText);
        } catch (NumberFormatException ex) {
            showStatus(langMgr.getString("login.error.numeric"), err);
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText(langMgr.getString("login.button.authenticating"));
        showStatus("", accent);

        executor.submit(() -> {
            try {
                Account account = authService.login(custNo, pin, bankName);
                SwingUtilities.invokeLater(() -> openDashboard(account));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showStatus(ex.getMessage(), err);
                    loginBtn.setEnabled(true);
                    loginBtn.setText(langMgr.getString("login.button.login"));
                    AppLogger.error("Login failed", ex);
                });
            }
        });
    }

    private void openDashboard(Account account) {
        DashboardFrame dash = new DashboardFrame(account, this);
        dash.setVisible(true);
        setVisible(false);
        
        String placeholder = langMgr.getString("login.placeholder.customer");
        custField.setText(placeholder);
        custField.setForeground(txtDim);
        pinField.setText("");
        loginBtn.setEnabled(true);
        loginBtn.setText(langMgr.getString("login.button.login"));
        showStatus(" ", err);
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg.isEmpty() ? " " : msg);
        statusLabel.setForeground(color);
    }

    // ── UI Helpers ──
    private JTextField styledTextField() {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                paintInputBg(g, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        applyFieldStyle(f);
        applyPlaceholder(f, "");
        return f;
    }

    private JPasswordField styledPinField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                paintInputBg(g, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        applyFieldStyle(f);
        return f;
    }

    private void paintInputBg(Graphics g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(inputBg);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 12, 12));
        g2.dispose();
    }

    private void applyFieldStyle(JTextField f) {
        f.setOpaque(false);
        f.setForeground(txt);
        f.setCaretColor(primary);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
            
        // Handle focus colors without removing all listeners
        if (f.getFocusListeners().length <= 2) { // Only add if not already present
            f.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(primary, 2, true),
                        new EmptyBorder(9, 13, 9, 13)));
                }
                @Override public void focusLost(FocusEvent e) {
                    f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(border, 1, true),
                        new EmptyBorder(10, 14, 10, 14)));
                }
            });
        }
    }

    private void applyPlaceholder(JTextField f, String ph) {
        f.setText(ph);
        f.setForeground(txtDim);
        f.putClientProperty("placeholder", ph);
        f.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                String currentPh = (String) f.getClientProperty("placeholder");
                if (currentPh != null && f.getText().equals(currentPh)) { 
                    f.setText(""); f.setForeground(txt); 
                }
            }
            @Override public void focusLost(FocusEvent e) {
                String currentPh = (String) f.getClientProperty("placeholder");
                if (currentPh != null && f.getText().isBlank()) { 
                    f.setText(currentPh); f.setForeground(txtDim); 
                }
            }
        });
    }
}
