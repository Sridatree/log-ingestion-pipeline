package com.analytics.LogProcessor.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CategoryValidator
 */
@ExtendWith(MockitoExtension.class)
class CategoryValidatorTest {

    private CategoryValidator validator;

    @Mock
    private ValidCategory validCategory;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new CategoryValidator();
    }

    @Test
    void testValidCategoryValues() {
        when(validCategory.allowNull()).thenReturn(false);
        validator.initialize(validCategory);

        // Test all valid categories (lowercase as per the API)
        assertTrue(validator.isValid("phishing", context));
        assertTrue(validator.isValid("contentinjection", context));
        assertTrue(validator.isValid("drivebycompromise", context));
        assertTrue(validator.isValid("exploitpublicfacingapplication", context));
        assertTrue(validator.isValid("externalremoteservices", context));
        assertTrue(validator.isValid("hardwareadditions", context));
        assertTrue(validator.isValid("replicationthroughremovablemedia", context));
        assertTrue(validator.isValid("supplychaincompromise", context));
        assertTrue(validator.isValid("trustedrelationship", context));
        assertTrue(validator.isValid("validaccounts", context));
    }

    @Test
    void testInvalidCategoryValues() {
        when(validCategory.allowNull()).thenReturn(false);
        validator.initialize(validCategory);

        assertFalse(validator.isValid("invalid-category", context));
        assertFalse(validator.isValid("malware", context));
        assertFalse(validator.isValid("", context));
        assertFalse(validator.isValid("unknown", context));
    }

    @Test
    void testNullValueWithAllowNullTrue() {
        when(validCategory.allowNull()).thenReturn(true);
        validator.initialize(validCategory);

        assertTrue(validator.isValid(null, context));
    }

    @Test
    void testNullValueWithAllowNullFalse() {
        when(validCategory.allowNull()).thenReturn(false);
        validator.initialize(validCategory);

        assertFalse(validator.isValid(null, context));
    }

    @Test
    void testCategoryWithWhitespace() {
        when(validCategory.allowNull()).thenReturn(false);
        validator.initialize(validCategory);

        // The Category.fromString normalizes by trimming and lowercasing
        assertTrue(validator.isValid("  phishing  ", context));
        assertTrue(validator.isValid("PHISHING", context));
        assertTrue(validator.isValid("Phishing", context));
    }

    @Test
    void testCategoryCaseInsensitive() {
        when(validCategory.allowNull()).thenReturn(false);
        validator.initialize(validCategory);

        assertTrue(validator.isValid("PHISHING", context));
        assertTrue(validator.isValid("Phishing", context));
        assertTrue(validator.isValid("phishing", context));
        assertTrue(validator.isValid("PhIsHiNg", context));
    }
}

