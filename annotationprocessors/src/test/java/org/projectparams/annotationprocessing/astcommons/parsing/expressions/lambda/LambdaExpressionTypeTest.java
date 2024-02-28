package org.projectparams.annotationprocessing.astcommons.parsing.expressions.lambda;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LambdaExpressionTypeTest {

    @Test
    void testMatches() {
        assertTrue(LambdaExpressionType.getInstance().matches("(a, b) -> a.execute(b))"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a, b) -> a.execute(b))",
            "(Cast) (a -> b)"
    })
    void testNotMatches(String expression) {
        assertFalse(LambdaExpressionType.getInstance().matches(expression));
    }
}