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
 * Unit tests for IpAddressValidator
 */
@ExtendWith(MockitoExtension.class)
class IpAddressValidatorTest {

    private IpAddressValidator validator;

    @Mock
    private ValidIpAddress validIpAddress;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new IpAddressValidator();
    }

    @Test
    void testValidIPv4Addresses() {
        when(validIpAddress.allowNull()).thenReturn(false);
        validator.initialize(validIpAddress);

        assertTrue(validator.isValid("192.168.1.1", context));
        assertTrue(validator.isValid("10.0.0.1", context));
        assertTrue(validator.isValid("172.16.0.1", context));
        assertTrue(validator.isValid("8.8.8.8", context));
        assertTrue(validator.isValid("255.255.255.255", context));
        assertTrue(validator.isValid("0.0.0.0", context));
        assertTrue(validator.isValid("127.0.0.1", context));
    }

    @Test
    void testValidIPv6Addresses() {
        when(validIpAddress.allowNull()).thenReturn(false);
        validator.initialize(validIpAddress);

        assertTrue(validator.isValid("2001:0db8:85a3:0000:0000:8a2e:0370:7334", context));
        assertTrue(validator.isValid("2001:db8:85a3::8a2e:370:7334", context));
        assertTrue(validator.isValid("::1", context));
        assertTrue(validator.isValid("::", context));
        assertTrue(validator.isValid("fe80::1", context));
    }

    @Test
    void testNullValue() {
        when(validIpAddress.allowNull()).thenReturn(false);
        validator.initialize(validIpAddress);

        // IpAddressValidator currently returns false for null
        assertFalse(validator.isValid(null, context));
    }

    @Test
    void testEmptyString() {
        when(validIpAddress.allowNull()).thenReturn(false);
        validator.initialize(validIpAddress);

        assertFalse(validator.isValid("", context));
        assertFalse(validator.isValid("   ", context));
    }

    @Test
    void testHostnames() {
        when(validIpAddress.allowNull()).thenReturn(false);
        validator.initialize(validIpAddress);

        // Valid hostnames should resolve and be valid
        assertTrue(validator.isValid("localhost", context));
    }


    @Test
    void testMalformedAddresses() {
        when(validIpAddress.allowNull()).thenReturn(false);
        validator.initialize(validIpAddress);

        assertFalse(validator.isValid("192.168.1.abc", context));
        assertFalse(validator.isValid("abc.def.ghi.jkl", context));
        assertFalse(validator.isValid("192.168..1", context));
        assertFalse(validator.isValid(".192.168.1.1", context));
        assertFalse(validator.isValid("192.168.1.1.", context));
    }
}

