package org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CastExpressionTypeTest {
    private final CastExpressionType castExpressionType = CastExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
            "(int)variableName",
            "(short)methodName()",
            "(SomeClass)newObject",
            "(java.lang.Integer)methodCall()",
            "(java.util.Map)someVar",
            "(ClasWithInner.InnerClass.AndNumber4.AndDefault)methodCall()",
            "(org.projectparams.SomeCustomClass)anotherMethodCall()"
    })
    void matchesTrueCases(String expression) {

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
        assertFalse(castExpressionType.matches(expression));
    }
}