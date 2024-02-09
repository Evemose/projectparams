package org.projectparams.annotationprocessing.astcommons.parsing.expressions.parenthezied;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

public class ParenthesizedExpressionTypeTest {
    
    private static final ParenthesizedExpressionType pet = ParenthesizedExpressionType.getInstance();

    @ParameterizedTest(name = "testMatches {index} with expression {0}")
    @ValueSource(strings = {
            "(validExpression)",
            "(a(b))",
            "(random[123])",
            "(1+2*3/4-5)"
    })
    public void testMatches(String expression) {
        // Test if the method can match valid expressions
        boolean result = pet.matches(expression);
        assertTrue(result, "The method did not match a valid expression");
    }

    @ParameterizedTest(name = "testNotMatches {index} with expression {0}")
    @ValueSource(strings = {
            "invalidExpression",
            "(invalidExpression",
            "invalidExpression)",
            "(Cast) methodInvocation()",
            "(someBoolean) ? (4) : (5)",
            "1+2*3/4-5"})
    public void testNotMatches(String expression) {
        // Test if the method does not match invalid expressions
        boolean result = pet.matches(expression);
        assertFalse(result, "The method matched an invalid expression");
    }
}