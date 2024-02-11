package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MethodInvocationExpressionTypeTest {

    private static final MethodInvocationExpressionType methodInvocationExpressionType = MethodInvocationExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
            "methodInvocation()",
            "complex.methodInvocation(arg1, arg2)",
            "comp$lex.methodInvocation(arg1, new AnotherClass()).method2()",
            "methodInvocation(arg).method2().method3()",
            "<Integer>genericMethodInvocation()",
            "new Complex<String, Integer>().methodInvocation(arg)",
            "<K, V>complex().methodInvocation(arg1).method2()",
            "methodInvocation.<String>genericMethod(arg).method2().method3()",
            "new org.projectparams.test.Sucus().mains[Sucus.mains[1].getZero()].<Map<Integer, List<Float>>>akakus(Map.of(3, List.of((float)(double)6.d)))"
    })
    void testMatch(String expression) {
        Assertions.assertTrue(methodInvocationExpressionType.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "methodInvocationWithoutEndingParenthesis",
            "(parenthesizedExpressionTypeInstance)",
            "new NewClassExpressionTypeInstance()",
            "(Cast) methodInvocation()",
            "<Integer>genericMethodInvocation",
            "methodInvocation.<String>genericMethod(arg).method2().",
            "someBoolean ? methodInvocation() : otherMethodInvocation()",
    })
    void testNotMatch(String expression) {
        Assertions.assertFalse(methodInvocationExpressionType.matches(expression));
    }
}