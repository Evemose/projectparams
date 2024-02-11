package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NewClassExpressionTypeTest {

    private final NewClassExpressionType newClassExpressionType = NewClassExpressionType.getInstance();

    @ParameterizedTest(name = "testMatches - Parametrized Test for {0}")
    @ValueSource(strings = {
            "new SomeClass()",
            "new SomeClass(param1, param2)",
            "new SomeClass<Generic>()",
            "new SomeClass<Generic>(param1, param2)",
            "owner.new SomeClass()",
            "abobus.new Bibus()",
            "methodCall().new SomeClass()",
            "<T>methodCall().new SomeClass(param1, param2)"
    })
    void testMatches(String expression) {
        Assertions.assertTrue(newClassExpressionType.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "newNotCorrectClass()",
            "noNewKeyword SomeClass()",
            "SomeClass<Generic>()",
            "fakenew SomeClass()",
            "(Cast) new SomeClass()",
            "someBoolean ? new SomeClass() : new SomeClass()",
            "new org.projectparams.test.Sucus().mains[Sucus.mains[1].getZero()]." +
                    "<Map<Integer, java.util.List<Float>>>akakus(Map.of(3, List.of((float)(double)6.d)))",
    })
    void testNotMatches(String expression) {
        Assertions.assertFalse(newClassExpressionType.matches(expression));
    }
}