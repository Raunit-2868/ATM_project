package com.atm.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * BankThemeManager - Handles the dynamic theming for different bank clones.
 */
public class BankThemeManager {

    public static class BankTheme {
        public final String name;
        public final Color primary;
        public final Color accent;
        public final Color card;
        public final Color inputBg;
        public final String logo;
        public final String header;
        public final String slogan;
        public final String logoPath;

        public BankTheme(String name, Color primary, Color accent, Color card, Color inputBg, String logo, String header, String slogan, String logoPath) {
            this.name = name;
            this.primary = primary;
            this.accent = accent;
            this.card = card;
            this.inputBg = inputBg;
            this.logo = logo;
            this.header = header;
            this.slogan = slogan;
            this.logoPath = logoPath;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final List<BankTheme> themes;
    private BankTheme currentTheme;

    private static BankThemeManager instance;

    private BankThemeManager() {
        themes = new ArrayList<>();
        
        // 1. SecureATM (Original Dark)
        themes.add(new BankTheme(
            "SecureATM (PRO)",
            new Color(108, 99, 255), // Primary
            new Color(0, 212, 170),  // Accent
            new Color(18, 18, 38),   // Card
            new Color(28, 28, 52),   // Input BG
            "🏧", "SecureATM", "Next-Gen Secure Banking",
            "/logos/secureatm_logo.png"
        ));

        // 2. SBI Clone (State Bank of India) - Premium Light Mode
        themes.add(new BankTheme(
            "SBI Bank",
            new Color(0, 112, 172),   // Authentic SBI Blue
            new Color(30, 30, 30),    // Dark Grey Accent/Text emphasis
            new Color(255, 255, 255), // Pure White Card
            new Color(240, 244, 250), // Soft Blue-Grey Input BG
            "🔵", "SBI ATM", "The Banker to Every Indian",
            "/logos/sbi_logo.png"
        ));

        // 3. HDFC Clone
        themes.add(new BankTheme(
            "HDFC Bank",
            new Color(0, 71, 187),   // HDFC Blue
            new Color(237, 28, 36),  // HDFC Red
            new Color(250, 250, 255),
            new Color(240, 242, 250),
            "🟦", "HDFC ATM", "We Understand Your World",
            "/logos/hdfc_logo.png"
        ));

        // 4. ICICI Clone
        themes.add(new BankTheme(
            "ICICI Bank",
            new Color(227, 112, 0),  // ICICI Orange
            new Color(0, 51, 102),   // ICICI Navy
            new Color(255, 252, 245),
            new Color(250, 245, 235),
            "🟠", "ICICI Bank", "Hum Hai Na, Khayal Apka",
            "/logos/icici_logo.png"
        ));

        currentTheme = themes.get(0); // Default to SecureATM
    }

    public static synchronized BankThemeManager getInstance() {
        if (instance == null) {
            instance = new BankThemeManager();
        }
        return instance;
    }

    public List<BankTheme> getThemes() {
        return themes;
    }

    public BankTheme getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(BankTheme theme) {
        this.currentTheme = theme;
    }
}
