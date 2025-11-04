package com.analytics.LogProcessor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Category enum
 */
class CategoryTest {

    @Test
    void testFromStringValidCategories() {
        assertEquals(Category.PHISHING, Category.fromString("phishing"));
        assertEquals(Category.CONTENT_INJECTION, Category.fromString("contentinjection"));
        assertEquals(Category.DRIVE_BY_COMPROMISE, Category.fromString("drivebycompromise"));
        assertEquals(Category.EXPLOIT_PUBLIC_FACING_APPLICATIONS, Category.fromString("exploitpublicfacingapplication"));
        assertEquals(Category.EXTERNAL_REMOTE_SERVICES, Category.fromString("externalremoteservices"));
        assertEquals(Category.HARDWARE_ADDITIONS, Category.fromString("hardwareadditions"));
        assertEquals(Category.REPLICATION_THROUGH_REMOVABLE_MEDIA, Category.fromString("replicationthroughremovablemedia"));
        assertEquals(Category.SUPPLY_CHAIN_COMPROMISE, Category.fromString("supplychaincompromise"));
        assertEquals(Category.TRUSTED_RELATIONSHIP, Category.fromString("trustedrelationship"));
        assertEquals(Category.VALID_ACCOUNTS, Category.fromString("validaccounts"));
    }

    @Test
    void testFromStringCaseInsensitive() {
        assertEquals(Category.PHISHING, Category.fromString("PHISHING"));
        assertEquals(Category.PHISHING, Category.fromString("Phishing"));
        assertEquals(Category.PHISHING, Category.fromString("PhIsHiNg"));
    }

    @Test
    void testFromStringWithWhitespace() {
        assertEquals(Category.PHISHING, Category.fromString("  phishing  "));
        assertEquals(Category.CONTENT_INJECTION, Category.fromString("  contentinjection  "));
    }

    @Test
    void testFromStringInvalidCategory() {
        assertThrows(IllegalArgumentException.class, () -> {
            Category.fromString("invalid-category");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Category.fromString("malware");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Category.fromString("");
        });
    }

    @Test
    void testIsValidWithValidCategories() {
        assertTrue(Category.isValid("phishing"));
        assertTrue(Category.isValid("contentinjection"));
        assertTrue(Category.isValid("PHISHING"));
        assertTrue(Category.isValid("  phishing  "));
    }

    @Test
    void testIsValidWithInvalidCategories() {
        assertFalse(Category.isValid("invalid-category"));
        assertFalse(Category.isValid("malware"));
        assertFalse(Category.isValid(""));
        assertFalse(Category.isValid(null));
    }

}

