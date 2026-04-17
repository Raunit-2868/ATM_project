package com.atm.ui;

import com.atm.db.AccountDAO;
import com.atm.model.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import com.atm.util.BankThemeManager;
import com.atm.util.BankThemeManager.BankTheme;

/**
 * TransactionHistoryDialog - Shows the last 20 transactions in a styled table.
 *
 * Uses SwingWorker to load data from SQLite on a background thread,
 * then updates the table model on the EDT once done.
 */
public class TransactionHistoryDialog extends JDialog {

    // ── Fields ─────────────────────────────────────────────────────────────────
    private final BankTheme theme = BankThemeManager.getInstance().getCurrentTheme();
    private final Color SUCCESS   = new Color(81, 207, 102);
    private final Color err       = new Color(255, 107, 107);

    // Theme-derived colors — light/dark adaptive, matching DashboardFrame logic
    private final boolean isDark  = theme.name.contains("PRO");
    private final Color primary   = theme.primary;
    private final Color accent    = theme.accent;
    private final Color cardBg    = theme.card;
    private final Color inputBg   = theme.inputBg;
    private final Color bg        = isDark ? new Color(8, 8, 20)    : new Color(244, 247, 250);
    private final Color txt       = isDark ? new Color(224, 224, 240): new Color(20, 25, 35);
    private final Color txtDim    = isDark ? new Color(120, 120, 160): new Color(100, 110, 125);

    private final Color tableBg   = theme.inputBg;
    private final Color rowAlt    = isDark ? new Color(20, 20, 42)  : new Color(235, 238, 245);
    private final Color hdrBg     = theme.primary;

    private final Account account;
    private final AccountDAO accountDAO = new AccountDAO();

    // ── Constructor ────────────────────────────────────────────────────────────
    public TransactionHistoryDialog(JFrame parent, Account account) {
        super(parent, "Transaction History — " + account.getBankName(), true);
        this.account = account;
        setupDialog();
        buildUI();
    }

    private void setupDialog() {
        setSize(680, 520);
        setLocationRelativeTo(getParent());
        setResizable(true);
        getContentPane().setBackground(bg);
    }

    // ── UI Construction ────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(bg);

        // ── Header ──
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(22, 26, 12, 26));

        JLabel title = new JLabel("📋  Transaction History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(txt);

        JLabel sub = new JLabel("Last 20 transactions · " + theme.name +
                " · Customer #" + account.getCustomerNumber());
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(txtDim);

        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(sub);

        add(header, BorderLayout.NORTH);

        // ── Table ──
        String[] cols = {"Account", "Action", "Amount", "Balance After", "Date & Time"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        // Loading placeholder
        model.addRow(new String[]{"", "Loading…", "", "", ""});

        JTable table = buildTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        scroll.setBackground(bg);
        scroll.getViewport().setBackground(tableBg);
        scroll.getVerticalScrollBar().setBackground(inputBg);

        add(scroll, BorderLayout.CENTER);

        // ── Load data on background thread via SwingWorker ──
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() {
                return accountDAO.getTransactionHistory(
                        account.getCustomerNumber(), account.getBankName());
            }

            @Override
            protected void done() {
                try {
                    model.setRowCount(0); // clear loading placeholder
                    List<String[]> rows = get();
                    if (rows.isEmpty()) {
                        model.addRow(new String[]{"—", "No transactions found", "—", "—", "—"});
                    } else {
                        rows.forEach(model::addRow);
                    }
                } catch (Exception e) {
                    model.setRowCount(0);
                    model.addRow(new String[]{"Error", "Failed to load history", "—", "—", "—"});
                }
            }
        }.execute();
    }

    // ── Table Builder ──────────────────────────────────────────────────────────
    private JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setBackground(tableBg);
        table.setForeground(txt);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(38);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFocusable(false);
        table.setSelectionBackground(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 80));
        table.setSelectionForeground(txt);
        table.setGridColor(isDark ? new Color(35, 35, 60) : new Color(210, 215, 225));
        table.setShowGrid(true);

        // Header
        JTableHeader th = table.getTableHeader();
        th.setBackground(hdrBg);
        th.setForeground(Color.WHITE);
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setPreferredSize(new Dimension(0, 42));
        th.setReorderingAllowed(false);
        th.setResizingAllowed(false);

        // Column widths
        int[] widths = {100, 120, 110, 120, 200};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Action column — color-coded (DEPOSIT=green, WITHDRAWAL=red, TRANSFER=accent)
        table.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(row % 2 == 0 ? tableBg : rowAlt);
                String v = val != null ? val.toString() : "";
                if (v.equalsIgnoreCase("DEPOSIT") || v.equalsIgnoreCase("TRANSFER IN")) {
                    setForeground(SUCCESS);
                } else if (v.equalsIgnoreCase("WITHDRAWAL") || v.equalsIgnoreCase("TRANSFER OUT")) {
                    setForeground(err);
                } else {
                    setForeground(txtDim);
                }
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                return this;
            }
        });

        // Default renderer for other columns
        DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                setBackground(row % 2 == 0 ? tableBg : rowAlt);
                setForeground(txt);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 1) table.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        return table;
    }
}
