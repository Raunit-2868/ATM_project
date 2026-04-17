package com.atm.util;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * LanguageManager - Singleton utility to handle application-wide localization.
 */
public class LanguageManager {
    private static LanguageManager instance;
    private ResourceBundle bundle;
    private Locale currentLocale;

    private LanguageManager() {
        // Default to English
        setLanguage("en");
    }

    public static synchronized LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void setLanguage(String langCode) {
        // Use Locale.forLanguageTag() — the new Locale(String) constructor is deprecated in Java 17+
        currentLocale = Locale.forLanguageTag(langCode.equals("hi") ? "hi" : "en");
        bundle = ResourceBundle.getBundle("messages", currentLocale);
    }

    public String getString(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return "!" + key + "!";
        }
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }
}
