package com.atm.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AppLogger - Simple thread-safe file + console logger.
 * Writes logs to atm.log next to the running binary (not the CWD).
 */
public class AppLogger {

    private static final String LOG_FILE = resolveLogPath();
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AppLogger() {}

    /** Resolves log path relative to the binary so it is stable regardless of CWD. */
    private static String resolveLogPath() {
        try {
            File codeLocation = new File(
                AppLogger.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File dir = codeLocation.isDirectory() ? codeLocation : codeLocation.getParentFile();
            File projectRoot = dir.getParentFile() != null ? dir.getParentFile() : dir;
            return projectRoot.getAbsolutePath() + File.separator + "atm.log";
        } catch (URISyntaxException e) {
            return "atm.log"; // safe fallback
        }
    }

    public static void info(String message) {
        log("INFO ", message, null);
    }

    public static void warn(String message) {
        log("WARN ", message, null);
    }

    public static void error(String message, Throwable t) {
        log("ERROR", message, t);
    }

    private static synchronized void log(String level, String message, Throwable t) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String entry = "[" + timestamp + "] [" + level + "] " + message;
        System.out.println(entry);
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            pw.println(entry);
            if (t != null) {
                t.printStackTrace(pw);
            }
        } catch (IOException e) {
            System.err.println("Logger write failed: " + e.getMessage());
        }
    }
}
