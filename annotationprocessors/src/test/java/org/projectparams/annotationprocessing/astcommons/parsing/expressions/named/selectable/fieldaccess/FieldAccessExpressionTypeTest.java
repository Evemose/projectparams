package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldAccessExpressionTypeTest {
    private final FieldAccessExpressionType fieldAccessExpressionType = new FieldAccessExpressionType();

    @ParameterizedTest
    @ValueSource(strings = {
            "test.variable",
            "test.variable.variable",
            "test.variable[0].variable",
            "test.variable().new Test().variable",
    })
    public void testMatches(String expression) {
        assertTrue(fieldAccessExpressionType.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "test()",
            "test[0]",
            "(String) test",
            "test.method()",
            "test.method[0]",
            "test.method<>()",
            "new Access.Test()",
    })
    public void testNotMatches(String expression) {
        assertFalse(fieldAccessExpressionType.matches(expression));
    }
}