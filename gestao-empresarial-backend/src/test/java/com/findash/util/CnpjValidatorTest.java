package com.findash.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CnpjValidatorTest {

    @Test
    void isValid_withValidCnpj_returnsTrue() {
        assertTrue(CnpjValidator.isValid("11222333000181"));
    }

    @Test
    void isValid_withFormattedCnpj_returnsTrue() {
        assertTrue(CnpjValidator.isValid("11.222.333/0001-81"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"11222333000182", "12345678901234", "abcdefghijklmn", "123", ""})
    void isValid_withInvalidCnpj_returnsFalse(String cnpj) {
        assertFalse(CnpjValidator.isValid(cnpj));
    }

    @Test
    void isValid_withAllSameDigits_returnsFalse() {
        assertFalse(CnpjValidator.isValid("11111111111111"));
    }

    @Test
    void isValid_withNull_returnsFalse() {
        assertFalse(CnpjValidator.isValid(null));
    }

    @Test
    void normalize_removesFormatting() {
        assertEquals("11222333000181", CnpjValidator.normalize("11.222.333/0001-81"));
    }

    @Test
    void normalize_keepsRawDigits() {
        assertEquals("11222333000181", CnpjValidator.normalize("11222333000181"));
    }

    @Test
    void format_addsFormatting() {
        assertEquals("11.222.333/0001-81", CnpjValidator.format("11222333000181"));
    }
}
