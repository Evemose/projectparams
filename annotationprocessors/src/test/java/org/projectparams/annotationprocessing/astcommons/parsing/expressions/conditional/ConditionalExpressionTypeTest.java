package org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ConditionalExpressionTypeTest {
    private static final ConditionalExpressionType conditionalExpressionType = new ConditionalExpressionType();

    @ParameterizedTest
    @ValueSource(strings = {
            "x ? y : z",
            "x ? y ? a : b : z",
            "x ? (y ? a : (b ? c : d)) : z",
            "(x ? y : z) ? a : b",
            "x ? (y > z ? a : b) : (c < d ? e : f)",
            "(x+y) ? ((a*b)/c ? d-e : f+g) : z-a",
            "a ? ((b && c) ? d : e) : f",
            "g ? (h ? (i ? j : k) : l) : m",
            "new org.projectparams.test.Sucus().mains[Sucus.mains[1].getZero()]." +
            "<Map<Integer, List<Float>>>akakus(Map.of(3, List.of((float)(double)6.d))).isEmpty() ? " +
                    "!(true ? false : true) ? akakus() : List.of(new HashMap<>(Map.of(3, List.of((float)(double)6.d))))" +
                    ": List.of(new HashMap<>(Map.of(3, List.of((float)(double)6.d))))"
    })
    public void testMatchesTrue(String input) {
        assertTrue(conditionalExpressionType.matches(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(x ? y) : z",
            "x",
            "((x+y) ? y*z : z-a)",
            "(a ? (b && c) ? d : e : f)"
    })
    public void testMatchesFalse(String input) {
        assertFalse(conditionalExpressionType.matches(input));
    }
}