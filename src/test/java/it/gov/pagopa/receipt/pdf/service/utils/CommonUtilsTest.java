package it.gov.pagopa.receipt.pdf.service.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

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

    @ParameterizedTest
    @MethodSource("sanitizeTestCases")
    void testSanitize_VariousInputs(String input, String expected) {
        assertEquals(expected, CommonUtils.sanitize(input));
    }

    private static Stream<Arguments> sanitizeTestCases() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("<script>alert('XSS')</script>", "&lt;script&gt;alert(&#x27;XSS&#x27;)&lt;&#x2F;script&gt;"),
                org.junit.jupiter.params.provider.Arguments.of("a\nb\rc\td", "abcd"),
                org.junit.jupiter.params.provider.Arguments.of("&<>", "&amp;&lt;&gt;"),
                org.junit.jupiter.params.provider.Arguments.of("abc\0def", "abcdef")
        );
    }
}

