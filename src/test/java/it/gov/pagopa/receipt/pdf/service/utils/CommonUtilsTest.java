package it.gov.pagopa.receipt.pdf.service.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonUtilsTest {

    @Test
    void testSanitize_NullInput() {
        assertNull(CommonUtils.sanitize(null));
    }

    @Test
    void testSanitize_EmptyString() {
        assertEquals("", CommonUtils.sanitize(""));
    }

    @Test
    void testSanitize_TrimWhitespace() {
        assertEquals("abc", CommonUtils.sanitize("  abc  "));
    }

    @Test
    void testSanitize_EscapeSpecialCharacters() {
        String input = "<script>alert('XSS')</script>";
        String expected = "&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;&#x2F;script&gt;";
        assertEquals(expected, CommonUtils.sanitize(input));
    }

    @Test
    void testSanitize_RemoveControlCharacters() {
        String input = "a\nb\rc\td";
        String expected = "abcd";
        assertEquals(expected, CommonUtils.sanitize(input));
    }

    @Test
    void testSanitize_EscapeAmpersandFirst() {
        String input = "&<>";
        String expected = "&amp;&lt;&gt;";
        assertEquals(expected, CommonUtils.sanitize(input));
    }

    @Test
    void testSanitize_RemoveNullChar() {
        String input = "abc\0def";
        String expected = "abcdef";
        assertEquals(expected, CommonUtils.sanitize(input));
    }
}

