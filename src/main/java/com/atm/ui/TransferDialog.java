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
 * TransferDialog - Modal dialog for transferring funds between Checking and Savings.
 * Runs the DB operation on a shared background executor thread.
 */
public class TransferDialog extends JDialog {

    private final BankTheme theme = BankThemeManager.getInstance().getCurrentTheme();
    private final Color SUCCESS = new Color(81, 207, 102);
    private final Color err     = new Color(255, 107, 107);

    private final boolean isDark = theme.name.contains("PRO");
    private final Color primary  = theme.primary;
    private final Color accent   = theme.accent;
    private final Color cardBg   = theme.card;
    private final Color inputBg  = theme.inputBg;
    private final Color bg       = isDark ? new Color(8,  8,  20)   : new Color(244, 247, 250);
    private final Color txt      = isDark ? new Color(224, 224, 240) : new Color(20, 25, 35);
    private final Color txtDim   = isDark ? new Color(120, 120, 160) : new Color(100, 110, 125);
    private final Color border   = isDark ? new Color(50, 50, 85)   : new Color(210, 215, 225);

    private final Account account;
    private final TransactionService txService;
    private final ExecutorService executor;

    private JComboBox<String> fromCombo;
    private JTextField        amountField;
    private JLabel            fromBalLbl;
    private JLabel            toBalLbl;
    private JLabel            statusLbl;
    private AnimatedButton    confirmBtn;

    public TransferDialog(JFrame parent, Account account,
                          TransactionService txService, ExecutorService executor) {
        super(parent, "Transfer Funds", true);
        this.account   = account;
        this.txService = txService;
        this.executor  = executor;
        setupDialog();
        buildUI();
    }

    private void setupDialog() {
        setSize(420, 470);
        setLocationRelativeTo(getParent());
        setResizable(false);
        getContentPane().setBackground(bg);
    }

    private void buildUI() {
        setLayout(new GridBagLayout());

        Color transferColor = new Color(108, 180, 255); // Soft blue for transfer

        RoundedPanel card = new RoundedPanel(22, cardBg);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));
        card.setPreferredSize(new Dimension(360, 410));

        // ── Title ──
        JLabel titleLbl = new JLabel("🔄  Transfer Funds", SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLbl.setForeground(transferColor);
        titleLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── From selector ──
        JLabel fromLabel = new JLabel("Transfer From:");
        fromLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        fromLabel.setForeground(txtDim);
        fromLabel.setAlignmentX(LEFT_ALIGNMENT);

        fromCombo = new JComboBox<>(new String[]{"Checking → Savings", "Savings → Checking"});
        fromCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fromCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        fromCombo.setAlignmentX(LEFT_ALIGNMENT);
        fromCombo.setBackground(inputBg);
        fromCombo.setForeground(txt);
        fromCombo.addActionListener(e -> updateBalanceLabels());

        // ── Balance display ──
        JPanel balPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        balPanel.setOpaque(false);
        balPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        balPanel.setAlignmentX(LEFT_ALIGNMENT);

        fromBalLbl = balLbl("From", account.getCheckingBalance());
        toBalLbl   = balLbl("To",   account.getSavingBalance());
        balPanel.add(fromBalLbl);
        balPanel.add(toBalLbl);

        // ── Amount field ──
        String currSymbol = account.getBankName().contains("SBI") ||
                            account.getBankName().contains("HDFC") ||
                            account.getBankName().contains("ICICI") ? "₹" : "$";
        JLabel amtLabel = new JLabel("Amount to Transfer (" + currSymbol + ")");
        amtLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        amtLabel.setForeground(txtDim);
        amtLabel.setAlignmentX(LEFT_ALIGNMENT);

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
        amountField.setCaretColor(transferColor);
        amountField.setFont(new Font("Segoe UI", Font.BOLD, 20));
        amountField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border, 1, true),
            new EmptyBorder(10, 14, 10, 14)));
        amountField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        amountField.setAlignmentX(LEFT_ALIGNMENT);
        amountField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                amountField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(transferColor, 2, true),
                    new EmptyBorder(9, 13, 9, 13)));
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                amountField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    new EmptyBorder(10, 14, 10, 14)));
            }
        });

        // ── Status ──
        statusLbl = new JLabel(" ", SwingConstants.CENTER);
        statusLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLbl.setForeground(err);
        statusLbl.setAlignmentX(CENTER_ALIGNMENT);

        // ── Confirm button ──
        confirmBtn = new AnimatedButton("TRANSFER", transferColor, transferColor.brighter());
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        confirmBtn.setAlignmentX(CENTER_ALIGNMENT);
        confirmBtn.addActionListener(e -> handleTransfer());

        // ── Assemble ──
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(20));
        card.add(fromLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(fromCombo);
        card.add(Box.createVerticalStrut(14));
        card.add(balPanel);
        card.add(Box.createVerticalStrut(18));
        card.add(amtLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(amountField);
        card.add(Box.createVerticalStrut(10));
        card.add(statusLbl);
        card.add(Box.createVerticalStrut(10));
        card.add(confirmBtn);

        add(card);
        SwingUtilities.invokeLater(() -> amountField.requestFocusInWindow());
    }

    private JLabel balLbl(String prefix, double amount) {
        JLabel lbl = new JLabel(prefix + ": " + CurrencyUtil.format(amount, account.getBankName()));
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(txtDim);
        lbl.setOpaque(false);
        return lbl;
    }

    private void updateBalanceLabels() {
        boolean checkingToSavings = fromCombo.getSelectedIndex() == 0;
        double from = checkingToSavings ? account.getCheckingBalance() : account.getSavingBalance();
        double to   = checkingToSavings ? account.getSavingBalance()   : account.getCheckingBalance();
        fromBalLbl.setText("From: " + CurrencyUtil.format(from, account.getBankName()));
        toBalLbl.setText("To: "   + CurrencyUtil.format(to,   account.getBankName()));
    }

    private void handleTransfer() {
        String raw = amountField.getText().trim()
                .replace(",", "").replace("$", "").replace("₹", "");
        if (raw.isEmpty()) { statusLbl.setText("Please enter an amount."); return; }

        double amount;
        try {
            amount = Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            statusLbl.setText("Enter a valid numeric amount.");
            return;
        }

        boolean checkingToSavings = fromCombo.getSelectedIndex() == 0;
        String fromType = checkingToSavings ? "CHECKING" : "SAVING";
        String toType   = checkingToSavings ? "SAVING"   : "CHECKING";

        confirmBtn.setEnabled(false);
        confirmBtn.setText("Processing…");

        executor.submit(() -> {
            try {
                double[] results = txService.transfer(account, fromType, toType, amount);
                SwingUtilities.invokeLater(() -> {
                    statusLbl.setForeground(SUCCESS);
                    statusLbl.setText("✅  Transfer complete!");
                    fromBalLbl.setText("From: " + CurrencyUtil.format(results[0], account.getBankName()));
                    toBalLbl.setText("To: "   + CurrencyUtil.format(results[1], account.getBankName()));
                    amountField.setText("");
                    confirmBtn.setEnabled(true);
                    confirmBtn.setText("TRANSFER AGAIN");
                    Timer t = new Timer(2000, ev -> dispose());
                    t.setRepeats(false);
                    t.start();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLbl.setForeground(err);
                    statusLbl.setText(ex.getMessage());
                    confirmBtn.setEnabled(true);
                    confirmBtn.setText("TRANSFER");
                });
            }
        });
    }
}
