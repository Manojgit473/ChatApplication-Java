package com.connectchat.util;

import java.util.regex.Pattern;

/**
 * Educational utility demonstrating Core Java Regular Expressions and String Manipulation.
 * 
 * Demonstrates:
 * - Precompiled java.util.regex.Pattern for high performance regex checks.
 * - Strict RFC-aligned email pattern matching.
 * - 10-digit Indian mobile number validation (starting with 6, 7, 8, or 9).
 * - Comprehensive strong password validation (length, uppercase, lowercase, digits, special characters).
 * - String trimming, sanitization, and HTML entity escaping.
 */
public final class StringValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,30}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]");

    private StringValidationUtils() {
    }

    public static boolean isValidUsername(String username) {
        if (username == null) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidIndianPhoneNumber(String phone) {
        if (phone == null) {
            return false;
        }
        String clean = PhoneNumberUtils.extract10Digit(phone);
        if (clean == null) {
            return false;
        }
        return INDIAN_PHONE_PATTERN.matcher(clean).matches();
    }

    /**
     * Validates strong password rules:
     * - Minimum 8 characters (up to 100)
     * - At least 1 uppercase letter (A-Z)
     * - At least 1 lowercase letter (a-z)
     * - At least 1 digit (0-9)
     * - At least 1 special character (!@#$%^&*()_+-=[]{}|;:,.<>/?~`)
     * 
     * @param password the candidate password
     * @return null if strong and valid, or a descriptive error message explaining which requirement is missing.
     */
    public static String validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            return "Password must be at least 8 characters long";
        }
        if (password.length() > 100) {
            return "Password cannot exceed 100 characters";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one uppercase letter (A-Z)";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter (a-z)";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number (0-9)";
        }
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return "Password must contain at least one special character (e.g. @, #, $, !, %, *, ?)";
        }
        return null;
    }

    public static boolean isStrongPassword(String password) {
        return validatePasswordStrength(password) == null;
    }

    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
