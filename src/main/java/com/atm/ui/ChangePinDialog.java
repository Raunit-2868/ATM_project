package com.atm.ui;

import com.atm.db.AccountDAO;
import com.atm.model.Account;
import com.atm.ui.components.AnimatedButton;
import com.atm.ui.components.RoundedPanel;
import com.atm.util.AppLogger;
import com.atm.util.BankThemeManager;
import com.atm.util.BankThemeManager.BankTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.ExecutorService;

/**
 * ChangePinDialog - Modal dialog for securely changing the account PIN.
 * Validates the current PIN, enforces 4-6 digit rules, and persists via AccountDAO.
 */
public class ChangePinDialog extends JDialog {

    private final BankTheme theme = BankThemeManager.getInstance().getCurrentTheme();
    private final Color SUCCESS = new Color(81, 207, 102);
    private final Color err     = new Color(255, 107, 107);

    private final boolean isDark = theme.name.contains("PRO");
    private final Color primary  = theme.primary;
    private final Color cardBg   = theme.card;
    private final Color inputBg  = theme.inputBg;
    private final Color bg       = isDark ? new Color(8, 8, 20)    : new Color(244, 247, 250);
    private final Color txt      = isDark ? new Color(224, 224, 240): new Color(20, 25, 35);
    private final Color txtDim   = isDark ? new Color(120, 120, 160): new Color(100, 110, 125);
    private final Color border   = isDark ? new Color(50, 50, 85)  : new Color(210, 215, 225);

    private final Account account;
    private final AccountDAO accountDAO = new AccountDAO();
    private final ExecutorService executor;

    private JPasswordField currentPinField;
    private JPasswordField newPinField;
    private JPasswordField confirmPinField;
    private JLabel         statusLbl;
    private AnimatedButton confirmBtn;

    public ChangePinDialog(JFrame parent, Account account, ExecutorService executor) {
        super(parent, "Change PIN", true);
        this.account  = account;
        this.executor = executor;
        setupDialog();
        buildUI();
    }

    private void setupDialog() {
        setSize(400, 460);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(bg);
    }

    private void buildUI() {
        setLayout(new GridBagLayout());

        Color pinColor = new Color(255, 180, 50); // Gold/amber for PIN

        RoundedPanel card = new RoundedPanel(22, cardBg);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));
        card.setPreferredSize(new Dimension(350, 400));

        // ── Title ──
        JLabel titleLbl = new JLabel("🔐  Change PIN", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(pinColor);
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subLbl = new JLabel("For: " + account.getOwnerName() + " · " + account.getBankName(),
                                   SwingConstants.CENTER);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(txtDim);
        subLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── Fields ──
        JLabel curLabel = new JLabel("Current PIN");
        curLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        curLabel.setForeground(txtDim);
        curLabel.setAlignmentX(LEFT_ALIGNMENT);
        currentPinField = styledPin(pinColor);

        JLabel newLabel = new JLabel("New PIN (4–6 digits)");
        newLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        newLabel.setForeground(txtDim);
        newLabel.setAlignmentX(LEFT_ALIGNMENT);
        newPinField = styledPin(pinColor);

        JLabel conLabel = new JLabel("Confirm New PIN");
        conLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        conLabel.setForeground(txtDim);
        conLabel.setAlignmentX(LEFT_ALIGNMENT);
        confirmPinField = styledPin(pinColor);

        // ── Status ──
        statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLbl.setForeground(err);
        statusLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── Button ──
        confirmBtn = new AnimatedButton("CHANGE PIN", pinColor, pinColor.brighter());
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        confirmBtn.setAlignmentX(CENTER_ALIGNMENT);
        confirmBtn.addActionListener(e -> handleChangePin());

        // ── Assemble ──
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(subLbl);
        card.add(Box.createVerticalStrut(24));
        card.add(curLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(currentPinField);
        card.add(Box.createVerticalStrut(16));
        card.add(newLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(newPinField);
        card.add(Box.createVerticalStrut(16));
        card.add(conLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(confirmPinField);
        card.add(Box.createVerticalStrut(14));
        card.add(statusLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(confirmBtn);

        add(card);
        SwingUtilities.invokeLater(() -> currentPinField.requestFocusInWindow());
    }

    private JPasswordField styledPin(Color accentColor) {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(inputBg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        f.setOpaque(false);
        f.setForeground(txt);
        f.setCaretColor(accentColor);
        f.setFont(new Font("Segoe UI", Font.BOLD, 18));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            new EmptyBorder(8, 14, 8, 14)));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(accentColor, 2, true),
                    new EmptyBorder(7, 13, 7, 13)));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    new EmptyBorder(8, 14, 8, 14)));
            }
        });
        return f;
    }

    private void handleChangePin() {
        String currentPinStr = new String(currentPinField.getPassword()).trim();
        String newPinStr     = new String(newPinField.getPassword()).trim();
        String confirmPinStr = new String(confirmPinField.getPassword()).trim();

        // Validation
        if (currentPinStr.isEmpty()) { showStatus("Please enter your current PIN.", err); return; }
        if (newPinStr.isEmpty())     { showStatus("Please enter a new PIN.", err); return; }
        if (!newPinStr.equals(confirmPinStr)) {
            showStatus("New PINs do not match. Please try again.", err);
            confirmPinField.setText("");
            return;
        }
        if (!newPinStr.matches("\\d{4,6}")) {
            showStatus("PIN must be 4–6 digits only.", err);
            newPinField.setText("");
            confirmPinField.setText("");
            return;
        }
        if (newPinStr.equals(currentPinStr)) {
            showStatus("New PIN must be different from current PIN.", err);
            return;
        }

        int currentPin, newPin;
        try {
            currentPin = Integer.parseInt(currentPinStr);
            newPin     = Integer.parseInt(newPinStr);
        } catch (NumberFormatException ex) {
            showStatus("PIN must be numeric only.", err);
            return;
        }

        confirmBtn.setEnabled(false);
        confirmBtn.setText("Updating…");

        executor.submit(() -> {
            boolean ok = accountDAO.updatePin(
                    account.getCustomerNumber(), account.getBankName(), currentPin, newPin);
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    // Update in-memory account so re-auth works in same session
                    account.setPinNumber(newPin);
                    showStatus("✅  PIN changed successfully!", SUCCESS);
                    AppLogger.info("PIN changed for customer " + account.getCustomerNumber());
                    currentPinField.setText("");
                    newPinField.setText("");
                    confirmPinField.setText("");
                    confirmBtn.setText("DONE ✓");
                    // Auto-close after 2s
                    Timer t = new Timer(2000, ev -> dispose());
                    t.setRepeats(false);
                    t.start();
                } else {
                    showStatus("❌  Incorrect current PIN. Please try again.", err);
                    currentPinField.setText("");
                    confirmBtn.setEnabled(true);
                    confirmBtn.setText("CHANGE PIN");
                }
            });
        });
    }

    private void showStatus(String msg, Color color) {
        statusLbl.setText(msg);
        statusLbl.setForeground(color);
    }
}
