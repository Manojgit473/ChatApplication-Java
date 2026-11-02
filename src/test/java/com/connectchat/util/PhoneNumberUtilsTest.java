package com.connectchat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhoneNumberUtilsTest {

    @Test
    void normalizeIndianPhoneNumber_With10Digits_ShouldPrefixCountryCode() {
        String result = PhoneNumberUtils.normalizeIndianPhoneNumber("9876543210");
        assertEquals("+91 9876543210", result);
    }

    @Test
    void normalizeIndianPhoneNumber_WithLeadingZero_ShouldNormalize() {
        String result = PhoneNumberUtils.normalizeIndianPhoneNumber("09876543210");
        assertEquals("+91 9876543210", result);
    }

    @Test
    void normalizeIndianPhoneNumber_WithCountryCode91_ShouldNormalize() {
        String result = PhoneNumberUtils.normalizeIndianPhoneNumber("+919876543210");
        assertEquals("+91 9876543210", result);

        String result2 = PhoneNumberUtils.normalizeIndianPhoneNumber("919876543210");
        assertEquals("+91 9876543210", result2);
    }

    @Test
    void extract10Digit_ShouldReturnDigitsOnly() {
        assertEquals("9876543210", PhoneNumberUtils.extract10Digit("+91 9876543210"));
        assertEquals("9876543210", PhoneNumberUtils.extract10Digit("9876543210"));
        assertEquals("9876543210", PhoneNumberUtils.extract10Digit("09876543210"));
    }
}
