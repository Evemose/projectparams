package org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class LiteralExpressionTypeTest {

    private final LiteralExpressionType testInstance = LiteralExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
            "123",
            "123.45",
            "true",
            "'a'",
            "\"test\"",
            "\"hello world\"",
            "123F",
            "123L",
            "9999D",
            "10e-3",
            "0x123",
            "0x123f",
            ".55f",
            "\"string with space\"",
            "\"multi\nline\"",
            "\"escaped\"quote\"",
            "1.67E-9d"})
    public void testMatchesInner_expectTrue(String input) {
        Assertions.assertTrue(testInstance.matches(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test",
            "123.45.67",
            "abc",
            "unescapedquote",
            "123LL",
            ".55Ff",
            "extra long char",
            "unclosed stringWithUnclosedQuote",
            "unclosed.charAtEnd",
            "escaped.Quote.\"In.middle",
            "multiline\nnoclosing.quote",
            "single.quote.in.middle",
            "2stringsin1",
            "multichars",
            "123-45"})
    public void testMatchesInner_expectFalse(String input) {
        Assertions.assertFalse(testInstance.matches(input));
    }
}