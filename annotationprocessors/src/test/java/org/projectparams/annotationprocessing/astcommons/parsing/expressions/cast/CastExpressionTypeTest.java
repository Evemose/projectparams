package org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CastExpressionTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "(int)variableName",
            "(short)methodName()",
            "(SomeClass)newObject",
            "(java.lang.Integer)methodCall()",
            "(java.util.Map)someVar",
            "(org.projectparams.SomeCustomClass)anotherMethodCall()"
    })
    void matchesTrueCases(String expression) {
        CastExpressionType castExpressionType = CastExpressionType.getInstance();
        assertTrue(castExpressionType.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "methodCall().string",
            "(int)",
            "(SomeClass)",
            "(SomeClass|AnotherClass)",
            "some.random.text",
            "(org.projectparams)"
    })
    void matchesFalseCases(String expression) {
        CastExpressionType castExpressionType = CastExpressionType.getInstance();
        assertFalse(castExpressionType.matches(expression));
    }
}