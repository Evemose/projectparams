package org.projectparams.annotationprocessing.astcommons.parsing.expressions.newarray;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class NewArrayExpressionTypeTest {

    private static final NewArrayExpressionType instance = NewArrayExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
            "new int[5]",
            "new int [5]",
            "new int[] {1, 2, 3}",
            "new String[5][6]",
            "new SomeClass5[6]",
            "new   float [ ][ ]  { { 1.1f , 2.2f } , { 3.3f , 4.4f } }",
            "new String [] [    ] { { \"Hello\" , \"World\" } , { \"Foo\" , \"Bar\" } }"
    })
    public void testMatches(String expression) {
        assertTrue(instance.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "not a new array expression",
            "newint[5]",
            "new int {1, 2, 3}",
    })
    public void testNotMatches(String expression) {
        assertFalse(instance.matches(expression));
    }
}