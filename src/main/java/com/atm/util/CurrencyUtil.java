package com.atm.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * CurrencyUtil - Thread-safe utility class for formatting monetary values.
 * Supports both INR (₹) for Indian banks and USD ($) for SecureATM PRO.
 */
public class CurrencyUtil {

    // ThreadLocal ensures each thread gets its own DecimalFormat instance (not thread-safe otherwise)
    private static final ThreadLocal<DecimalFormat> USD_FORMAT = ThreadLocal.withInitial(() ->
            new DecimalFormat("'$'###,##0.00", DecimalFormatSymbols.getInstance(Locale.US)));

    private static final ThreadLocal<DecimalFormat> INR_FORMAT = ThreadLocal.withInitial(() ->
            new DecimalFormat("'₹'##,##,##0.00", DecimalFormatSymbols.getInstance(Locale.US)));

    private CurrencyUtil() {}

    /**
     * Formats a double using the appropriate currency symbol for the given bank.
     * Indian banks (SBI, HDFC, ICICI) use ₹; SecureATM PRO uses $.
     */
    public static String format(double amount, String bankName) {
        if (bankName != null && (bankName.contains("SBI") ||
                bankName.contains("HDFC") || bankName.contains("ICICI"))) {
            return INR_FORMAT.get().format(amount);
        }
        return USD_FORMAT.get().format(amount);
    }

    /**
     * Formats a double as USD ($ format) — for use where bank context is unavailable.
     * @deprecated Prefer {@link #format(double, String)} to get the right currency symbol.
     */
    @Deprecated
    public static String format(double amount) {
        return USD_FORMAT.get().format(amount);
    }
}
