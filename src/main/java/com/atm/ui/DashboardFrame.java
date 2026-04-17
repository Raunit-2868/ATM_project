package com.atm.ui;

import com.atm.db.AccountDAO;
import com.atm.model.Account;
import com.atm.service.SessionManager;
import com.atm.service.TransactionService;
import com.atm.ui.components.AnimatedButton;
import com.atm.ui.components.RoundedPanel;
import com.atm.util.AppLogger;
import com.atm.util.CurrencyUtil;
import com.atm.util.BankThemeManager;
import com.atm.util.BankThemeManager.BankTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DashboardFrame - Main screen shown after successful login.
 *
 * Multithreading highlights:
 *  - SessionManager runs on a dedicated ScheduledExecutorService daemon thread.
 *  - All DB operations (via TransactionService) are submitted to a cached executor pool.
 *  - Balance refresh re-reads from DB on a background thread to stay current.
 *  - UI updates always dispatched back to the EDT via SwingUtilities.invokeLater.
 */
public class DashboardFrame extends JFrame {

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final BankTheme theme = BankThemeManager.getInstance().getCurrentTheme();
    private final Color SUCCESS     = new Color(81,  207, 102);
    private final Color DANGER      = new Color(255, 100,  80);

    // Theme-derived colors
    private final Color primary   = theme.primary;
    private final Color accent    = theme.accent;
    private final Color cardBg    = theme.card;
    private final Color inputBg   = theme.inputBg;
    private final boolean isDark  = theme.name.contains("PRO");
    private final Color bg        = isDark ? new Color(8, 8, 20)    : new Color(244, 247, 250);
    private final Color txt       = isDark ? new Color(224, 224, 240): new Color(20, 25, 35);
    private final Color txtDim    = isDark ? new Color(120, 120, 160): new Color(100, 110, 125);

    private Account account;
    private final JFrame loginFrame;

    private JLabel checkBalLbl;
    private JLabel saveBalLbl;
    private JLabel timerLbl;
    private boolean isBalanceHidden = true;

    private SessionManager sessionManager;

    private final TransactionService txService = new TransactionService();
    private final AccountDAO         accountDAO = new AccountDAO();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ATM-Worker");
        t.setDaemon(true);
        return t;
    });

    // ── Constructor ────────────────────────────────────────────────────────────
    public DashboardFrame(Account account, JFrame loginFrame) {
        this.account    = account;
        this.loginFrame = loginFrame;
        configFrame();
        buildUI();
        startSession();
    }

    // ── Frame Setup ────────────────────────────────────────────────────────────
    private void configFrame() {
        setTitle(theme.header + " — " + account.getOwnerName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(640, 760);
        setMinimumSize(new Dimension(560, 700));
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(bg);
    }

    // ── UI Construction ────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(28, 28, 20, 28));

        root.add(buildTopBar(),    BorderLayout.NORTH);
        root.add(buildCenter(),    BorderLayout.CENTER);
        root.add(buildBottomBar(), BorderLayout.SOUTH);

        add(root);
    }

    // ── Top Bar (greeting + back + session timer) ─────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 22, 0));

        // Back button
        JButton backBtn = new JButton("←");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        backBtn.setForeground(accent);
        backBtn.setBorderPainted(false);
        backBtn.setContentAreaFilled(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> logout());

        // Greeting
        JPanel greet = new JPanel();
        greet.setOpaque(false);
        greet.setLayout(new BoxLayout(greet, BoxLayout.Y_AXIS));

        JLabel welcome = new JLabel("Welcome back,");
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        welcome.setForeground(txtDim);

        JLabel name = new JLabel(account.getOwnerName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 22));
        name.setForeground(txt);

        JLabel bankID = new JLabel("🏦 " + theme.header);
        bankID.setFont(new Font("Segoe UI", Font.ITALIC | Font.BOLD, 12));
        bankID.setForeground(accent);

        greet.add(welcome);
        greet.add(name);
        greet.add(Box.createVerticalStrut(2));
        greet.add(bankID);

        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftSide.setOpaque(false);
        leftSide.add(backBtn);
        leftSide.add(Box.createHorizontalStrut(15));
        leftSide.add(greet);

        // Timer chip
        timerLbl = new JLabel("⏱  3:00");
        timerLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        timerLbl.setForeground(accent);
        timerLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        bar.add(leftSide, BorderLayout.WEST);
        bar.add(timerLbl, BorderLayout.EAST);
        return bar;
    }

    // ── Center panel ──────────────────────────────────────────────────────────
    private JPanel buildCenter() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Balance cards row
        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setOpaque(false);
        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        cards.add(buildBalanceCard("Checking Account", account.getCheckingBalance(), primary, true));
        cards.add(buildBalanceCard("Savings Account",  account.getSavingBalance(),   accent,  false));

        // Info strip
        RoundedPanel info = buildInfoStrip();
        info.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        // Action grid
        JPanel actions = buildActions();

        center.add(cards);
        center.add(Box.createVerticalStrut(18));
        center.add(info);
        center.add(Box.createVerticalStrut(18));
        center.add(actions);

        return center;
    }

    // ── Balance Card ──────────────────────────────────────────────────────────
    private RoundedPanel buildBalanceCard(String title, double balance,
                                          Color accentColor, boolean isChecking) {
        RoundedPanel card = new RoundedPanel(20, cardBg);
        card.setLayout(new BorderLayout());

        if (!isDark) {
            card.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 240), 1));
        }

        // Top accent stripe
        JPanel stripe = new JPanel();
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(0, 5));
        card.add(stripe, BorderLayout.NORTH);

        // Content
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(14, 18, 18, 18));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLbl.setForeground(txtDim);

        // Header panel for title + eye button
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titleLbl, BorderLayout.WEST);

        if (isChecking) {
            JButton toggleBtn = new JButton(isBalanceHidden ? "👁" : "🙈");
            toggleBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
            toggleBtn.setForeground(txtDim);
            toggleBtn.setBorderPainted(false);
            toggleBtn.setContentAreaFilled(false);
            toggleBtn.setFocusPainted(false);
            toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            toggleBtn.addActionListener(e -> {
                isBalanceHidden = !isBalanceHidden;
                toggleBtn.setText(isBalanceHidden ? "👁" : "🙈");
                updateBalanceDisplay();
            });
            header.add(toggleBtn, BorderLayout.EAST);
        }

        JLabel balLbl = new JLabel(isBalanceHidden ? "••••••" :
                CurrencyUtil.format(balance, account.getBankName()));
        balLbl.setFont(new Font("Segoe UI", Font.BOLD, 24));
        balLbl.setForeground(accentColor);

        if (isChecking) checkBalLbl = balLbl;
        else            saveBalLbl  = balLbl;

        JLabel sub = new JLabel("Customer #" + account.getCustomerNumber());
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(txtDim);

        body.add(header);
        body.add(Box.createVerticalStrut(6));
        body.add(balLbl);
        body.add(Box.createVerticalStrut(3));
        body.add(sub);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ── Info Strip ────────────────────────────────────────────────────────────
    private RoundedPanel buildInfoStrip() {
        RoundedPanel strip = new RoundedPanel(18, cardBg);
        strip.setLayout(new GridLayout(1, 3, 0, 0));

        if (!isDark) {
            strip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 240), 1),
                new EmptyBorder(13, 21, 13, 21)
            ));
        } else {
            strip.setBorder(new EmptyBorder(14, 22, 14, 22));
        }

        strip.add(infoCell("Bank", account.getBankName()));
        strip.add(infoCell("Customer ID", String.valueOf(account.getCustomerNumber())));
        strip.add(infoCell("Account Status", "✅  Active"));

        return strip;
    }

    private JPanel infoCell(String label, String value) {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(txtDim);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(txt);

        cell.add(lbl);
        cell.add(Box.createVerticalStrut(4));
        cell.add(val);
        return cell;
    }

    // ── Action Buttons ────────────────────────────────────────────────────────
    private JPanel buildActions() {
        // Row 1: Deposit buttons
        JPanel depositRow = new JPanel(new GridLayout(1, 2, 12, 0));
        depositRow.setOpaque(false);
        depositRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        depositRow.add(actionBtn("💰  Deposit — Checking", primary,        () -> openTx("CHECKING", "DEPOSIT")));
        depositRow.add(actionBtn("💰  Deposit — Savings",  accent,         () -> openTx("SAVING",   "DEPOSIT")));

        // Row 2: Withdraw buttons
        JPanel withdrawRow = new JPanel(new GridLayout(1, 2, 12, 0));
        withdrawRow.setOpaque(false);
        withdrawRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68));
        withdrawRow.add(actionBtn("🏧  Withdraw — Checking", primary.darker(), () -> openTx("CHECKING", "WITHDRAWAL")));
        withdrawRow.add(actionBtn("🏧  Withdraw — Savings",  accent.darker(),  () -> openTx("SAVING",   "WITHDRAWAL")));

        // Row 3: Transfer + Change PIN
        JPanel utilRow = new JPanel(new GridLayout(1, 2, 12, 0));
        utilRow.setOpaque(false);
        utilRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));

        AnimatedButton transferBtn = new AnimatedButton(
                "🔄  Transfer Funds", new Color(60, 140, 220), new Color(80, 160, 240));
        transferBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        transferBtn.addActionListener(e -> { sessionManager.resetTimer(); openTransfer(); });

        AnimatedButton pinBtn = new AnimatedButton(
                "🔐  Change PIN", new Color(180, 130, 20), new Color(210, 160, 30));
        pinBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pinBtn.addActionListener(e -> { sessionManager.resetTimer(); openChangePin(); });

        utilRow.add(transferBtn);
        utilRow.add(pinBtn);

        // Row 4: History
        AnimatedButton histBtn = new AnimatedButton(
            "📋  View Transaction History", inputBg, inputBg.darker());
        histBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        histBtn.setForeground(txt);
        histBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        histBtn.addActionListener(e -> { sessionManager.resetTimer(); openHistory(); });

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.add(depositRow);
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(withdrawRow);
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(utilRow);
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(histBtn);
        return wrapper;
    }

    private AnimatedButton actionBtn(String label, Color color, Runnable action) {
        AnimatedButton btn = new AnimatedButton(label, color, color.brighter());
        btn.setPreferredSize(new Dimension(0, 68));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.addActionListener(e -> { sessionManager.resetTimer(); action.run(); });
        return btn;
    }

    // ── Bottom Bar (Logout) ───────────────────────────────────────────────────
    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(14, 0, 0, 0));

        AnimatedButton logoutBtn = new AnimatedButton(
            "🚪  Logout", new Color(140, 40, 40), DANGER);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 44));
        logoutBtn.addActionListener(e -> logout());

        bar.add(logoutBtn, BorderLayout.CENTER);
        return bar;
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void openTx(String accountType, String action) {
        TransactionDialog dlg = new TransactionDialog(
            this, account, accountType, action, txService, executor);
        dlg.setVisible(true);
        refreshBalances(); // real DB refresh after dialog closes
    }

    private void openTransfer() {
        TransferDialog dlg = new TransferDialog(this, account, txService, executor);
        dlg.setVisible(true);
        refreshBalances();
    }

    private void openChangePin() {
        ChangePinDialog dlg = new ChangePinDialog(this, account, executor);
        dlg.setVisible(true);
    }

    private void openHistory() {
        new TransactionHistoryDialog(this, account).setVisible(true);
    }

    // ── Balance Refresh (real DB read on background thread) ───────────────────
    private void refreshBalances() {
        new SwingWorker<Account, Void>() {
            @Override
            protected Account doInBackground() {
                // Re-read fresh data from SQLite on the worker thread
                return accountDAO.refreshAccount(
                        account.getCustomerNumber(), account.getBankName());
            }

            @Override
            protected void done() {
                try {
                    Account fresh = get();
                    if (fresh != null) {
                        // Sync in-memory account with DB values
                        account.setCheckingBalance(fresh.getCheckingBalance());
                        account.setSavingBalance(fresh.getSavingBalance());
                    }
                    updateBalanceDisplay();
                } catch (Exception ignored) {
                    updateBalanceDisplay(); // show cached values if DB read fails
                }
            }
        }.execute();
    }

    private void updateBalanceDisplay() {
        if (isBalanceHidden) {
            checkBalLbl.setText("••••••");
            saveBalLbl.setText("••••••");
        } else {
            checkBalLbl.setText(CurrencyUtil.format(account.getCheckingBalance(), account.getBankName()));
            saveBalLbl.setText(CurrencyUtil.format(account.getSavingBalance(), account.getBankName()));
        }
    }

    // ── Session (Multithreading) ──────────────────────────────────────────────
    private void startSession() {
        sessionManager = new SessionManager(
            () -> SwingUtilities.invokeLater(this::handleSessionExpiry));
        sessionManager.setOnTickCallback(
            () -> SwingUtilities.invokeLater(this::updateTimerLabel));
        sessionManager.start();
    }

    private void updateTimerLabel() {
        int secs = sessionManager.getRemainingSeconds();
        int m    = secs / 60;
        int s    = secs % 60;
        timerLbl.setText(String.format("⏱  %d:%02d", m, s));
        timerLbl.setForeground(secs <= 30 ? Color.RED
                             : secs <= 60 ? new Color(255, 165, 0)
                             : accent);
    }

    private void handleSessionExpiry() {
        JOptionPane.showMessageDialog(
            this,
            "Your session expired due to inactivity.\nPlease log in again.",
            "Session Expired", JOptionPane.WARNING_MESSAGE);
        logout();
    }

    private void logout() {
        sessionManager.stop();
        executor.shutdownNow();
        AppLogger.info("Logout: customer " + account.getCustomerNumber());
        dispose();
        loginFrame.setVisible(true);
    }
}
