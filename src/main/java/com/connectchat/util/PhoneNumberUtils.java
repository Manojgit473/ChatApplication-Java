package com.connectchat.util;

/**
 * Utility for Indian phone number normalization and formatting.
 * Default country code for Connect Chat is India (+91).
 */
public final class PhoneNumberUtils {

    private PhoneNumberUtils() {
    }

    /**
     * Normalizes an Indian phone number to standard international format: "+91 XXXXXXXXXX".
     * Accepts:
     * - 10 digits: "9876543210" -> "+91 9876543210"
     * - 11 digits with leading 0: "09876543210" -> "+91 9876543210"
     * - 12 digits with 91: "919876543210" -> "+91 9876543210"
     * - With +91: "+919876543210" or "+91 9876543210" -> "+91 9876543210"
     *
     * Non-Indian international numbers are trimmed and preserved as-is.
     */
    public static String normalizeIndianPhoneNumber(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }

        String cleaned = raw.trim();
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");

        // 10-digit Indian mobile number
        if (digitsOnly.length() == 10) {
            return "+91 " + digitsOnly;
        }

        // 11 digits starting with 0 (e.g. STD/mobile prefix)
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("0")) {
            return "+91 " + digitsOnly.substring(1);
        }

        // 12 digits starting with 91
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+91 " + digitsOnly.substring(2);
        }

        // Has +91 already
        if (cleaned.startsWith("+91")) {
            String remainder = cleaned.substring(3).replaceAll("[^0-9]", "");
            if (remainder.length() == 10) {
                return "+91 " + remainder;
            }
        }

        return cleaned;
    }

    /**
     * Extracts the 10-digit mobile number if available.
     * e.g. "+91 9876543210" -> "9876543210"
     */
    public static String extract10Digit(String phone) {
        if (phone == null) {
            return null;
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return digits;
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        return null;
    }
}
