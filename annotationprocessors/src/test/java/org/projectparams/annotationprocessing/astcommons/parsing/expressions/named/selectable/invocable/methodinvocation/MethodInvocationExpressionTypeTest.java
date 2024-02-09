package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MethodInvocationExpressionTypeTest {

    private static final MethodInvocationExpressionType methodInvocationExpressionType = MethodInvocationExpressionType.getInstance();

    @ParameterizedTest(name = "should match the expression: {0}")
    @ValueSource(strings = {
            "methodInvocation()",
            "complex.methodInvocation(arg1, arg2)",
            "complex.methodInvocation(arg1, new AnotherClass()).method2()",
            "methodInvocation(arg).method2().method3()",
            "<Integer>genericMethodInvocation()",
            "new Complex<String, Integer>().methodInvocation(arg)",
            "<K, V>complex().methodInvocation(arg1).method2()",
            "methodInvocation.<String>genericMethod(arg).method2().method3()"
    })
    void testMatch(String expression) {
        var result = methodInvocationExpressionType.matches(expression);
        Assertions.assertTrue(result);
    }

    @ParameterizedTest(name = "should not match the expression: {0}")
    @ValueSource(strings = {
            "methodInvocationWithoutEndingParenthesis",
            "(parenthesizedExpressionTypeInstance)",
            "new NewClassExpressionTypeInstance()",
            "methodInvocation(arg1, arg2",
            "(Cast) methodInvocation()",
            "complex.methodInvocation(arg1,",
            "<Integer>genericMethodInvocation",
            "<String, Integer>complex.methodInvocation(arg",
            "<K, V>complex.methodInvocation(arg1).method2(",
            "methodInvocation.<String>genericMethod(arg).method2()."
    })
    void testNotMatch(String expression) {
        var result = methodInvocationExpressionType.matches(expression);
        Assertions.assertFalse(result, "The expression should not be matched as its either not ending with ')' or matches other types.");
    }
}