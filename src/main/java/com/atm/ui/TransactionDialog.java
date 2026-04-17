package com.atm.ui;

import com.atm.model.Account;
import com.atm.service.TransactionService;
import com.atm.ui.components.AnimatedButton;
import com.atm.ui.components.RoundedPanel;
import com.atm.util.CurrencyUtil;
import com.atm.util.BankThemeManager;
import com.atm.util.BankThemeManager.BankTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.ExecutorService;

/**
 * TransactionDialog - Modal dialog for Deposit or Withdrawal operations.
 *
 * The actual DB operation runs on the shared executor (background thread).
 * On success, shows the new balance and auto-closes after 2 seconds.
 * On failure, shows an inline error in red — no modal alert pop-up.
 */
public class TransactionDialog extends JDialog {

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final BankTheme theme = BankThemeManager.getInstance().getCurrentTheme();
    private final Color SUCCESS = new Color(81, 207, 102);

    // Theme-derived colors — resolved directly from theme, no stale static import
    private final Color primary  = theme.primary;
    private final Color accent   = theme.accent;
    private final Color cardBg   = theme.card;
    private final Color inputBg  = theme.inputBg;
    private final Color err      = new Color(255, 107, 107);

    // Light/dark adaptive colors (matching DashboardFrame logic)
    private final boolean isDark = theme.name.contains("PRO");
    private final Color bg      = isDark ? new Color(8,  8,  20)  : new Color(244, 247, 250);
    private final Color txt     = isDark ? new Color(224, 224, 240) : new Color(20, 25, 35);
    private final Color txtDim  = isDark ? new Color(120, 120, 160) : new Color(100, 110, 125);
    private final Color border  = isDark ? new Color(50, 50, 85)  : new Color(210, 215, 225);

    private final Account account;
    private final String accountType;
    private final String action;
    private final TransactionService txService;
    private final ExecutorService executor;

    private JTextField     amountField;
    private JLabel         statusLbl;
    private JLabel         balanceLbl;
    private AnimatedButton confirmBtn;

    // ── Constructor ────────────────────────────────────────────────────────────
    public TransactionDialog(JFrame parent, Account account, String accountType,
                             String action, TransactionService txService,
                             ExecutorService executor) {
        super(parent, buildTitle(accountType, action), true);
        this.account     = account;
        this.accountType = accountType;
        this.action      = action;
        this.txService   = txService;
        this.executor    = executor;
        setupDialog();
        buildUI();
    }

    private static String buildTitle(String type, String act) {
        String a = act.equals("DEPOSIT") ? "Deposit" : "Withdraw";
        String t = type.equals("CHECKING") ? "Checking" : "Savings";
        return a + " — " + t + " Account";
    }

    // ── Dialog Setup ───────────────────────────────────────────────────────────
    private void setupDialog() {
        setSize(400, 400);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(bg);
    }

    // ── UI Construction ────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new GridBagLayout());

        boolean isDeposit   = action.equals("DEPOSIT");
        Color   actionColor = isDeposit ? accent : err;
        String  emoji       = isDeposit ? "💰" : "🏧";

        RoundedPanel card = new RoundedPanel(22, cardBg);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 28, 28, 28));
        card.setPreferredSize(new Dimension(340, 340));

        // ── Title ──
        JLabel titleLbl = new JLabel(emoji + "  " + (isDeposit ? "Deposit" : "Withdraw"),
                                     SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(actionColor);
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── Current balance ──
        double cur = accountType.equals("CHECKING")
                ? account.getCheckingBalance() : account.getSavingBalance();
        balanceLbl = new JLabel("Balance: " + CurrencyUtil.format(cur, account.getBankName()),
                                SwingConstants.CENTER);
        balanceLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        balanceLbl.setForeground(txtDim);
        balanceLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── Amount label ──
        String currSymbol = account.getBankName().contains("SBI") ||
                            account.getBankName().contains("HDFC") ||
                            account.getBankName().contains("ICICI") ? "₹" : "$";
        JLabel amtLabel = new JLabel("Enter Amount (" + currSymbol + ")");
        amtLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        amtLabel.setForeground(txtDim);
        amtLabel.setAlignmentX(LEFT_ALIGNMENT);

        // ── Amount field ──
        amountField = new JTextField() {
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
        amountField.setOpaque(false);
        amountField.setForeground(txt);
        amountField.setCaretColor(actionColor);
        amountField.setFont(new Font("Segoe UI", Font.BOLD, 20));
        amountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        amountField.setAlignmentX(LEFT_ALIGNMENT);
        amountField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                amountField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(actionColor, 2, true),
                    new EmptyBorder(9, 13, 9, 13)));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                amountField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            }
        });

        // ── Status label ──
        statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLbl.setForeground(err);
        statusLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── Confirm button ──
        String btnText = isDeposit ? "DEPOSIT" : "WITHDRAW";
        confirmBtn = new AnimatedButton(btnText, actionColor, actionColor.brighter());
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        confirmBtn.setAlignmentX(CENTER_ALIGNMENT);
        confirmBtn.addActionListener(e -> handleTransaction());

        // ── Assemble ──
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(balanceLbl);
        card.add(Box.createVerticalStrut(22));
        card.add(amtLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(amountField);
        card.add(Box.createVerticalStrut(12));
        card.add(statusLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(confirmBtn);

        add(card);
        SwingUtilities.invokeLater(() -> amountField.requestFocusInWindow());
    }

    // ── Action ─────────────────────────────────────────────────────────────────
    private void handleTransaction() {
        String raw = amountField.getText().trim()
                .replace(",", "").replace("$", "").replace("₹", "");
        if (raw.isEmpty()) { showStatus("Please enter an amount.", err); return; }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showStatus("Enter a valid numeric amount.", err);
            return;
        }

        confirmBtn.setEnabled(false);
        confirmBtn.setText("Processing…");

        executor.submit(() -> {
            try {
                double newBal = action.equals("DEPOSIT")
                    ? txService.deposit(account, accountType, amount)
                    : txService.withdraw(account, accountType, amount);

                SwingUtilities.invokeLater(() -> {
                    showStatus("✅  Success! New Balance: " +
                            CurrencyUtil.format(newBal, account.getBankName()), SUCCESS);
                    balanceLbl.setText("Balance: " +
                            CurrencyUtil.format(newBal, account.getBankName()));
                    amountField.setText("");
                    confirmBtn.setEnabled(true);
                    confirmBtn.setText(action.equals("DEPOSIT") ? "DEPOSIT AGAIN" : "WITHDRAW AGAIN");
                    // Auto-close after 2 s
                    Timer t = new Timer(2000, ev -> dispose());
                    t.setRepeats(false);
                    t.start();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    showStatus(ex.getMessage(), err);
                    confirmBtn.setEnabled(true);
                    confirmBtn.setText(action.equals("DEPOSIT") ? "DEPOSIT" : "WITHDRAW");
                });
            }
        });
    }

    private void showStatus(String msg, Color color) {
        statusLbl.setText(msg.isEmpty() ? " " : msg);
        statusLbl.setForeground(color);
    }
}
