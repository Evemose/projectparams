package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BinaryExpressionTypeTest {

    BinaryExpressionType binaryExpressionType = BinaryExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
            "new HashMap<String, Integer>()",
            "<String, Integer>someMethod()",
            "SomeOwner.<String, Integer>someMethod().someField",
            "SomeOwner<String, Integer>.someMethod()",
            "SomeOwner<String, Integer>.<String, Integer>someMethod()",
            "main(args)",
            "(3 > 5)",
            "new String[]",
            "new String[3]",
            "new String[3][3]",
            "new String[3+2]",
            "(4 + getNumber())",
            "Integer.parseInt(\"3\")",
            "new String(\"3\")",
            "++i",
            "--i",
            "i++",
            "i--",
            "org.projectparams.test.Sucus().mains[Sucus.mains[1].getZero()].<Map<Integer, java.util.List<Float>>>akakus(Map.of(3, List.of((float)(double)6.d)))"
    })
    public void testMatchesNonBinaryExpression(String expression) {
        assertFalse(binaryExpressionType.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "get(3+2) + 2",
            "3 + 2",
            "3 - 2",
            "3 * 2",
            "access[2] / 2",
            "getOne(3 < 2) > 1",
            "3 % 2",
            "3 & 2",
            "3 | 2",
            "3 ^ 2",
            "3 << 2",
            "3 >> 2",
            "3 >>> 2",
            "3 < 2",
            "3 > 2",
            "3 <= 2",
            "3 >= 2",
            "3 == 2",
            "3 != 2",
            "true && false",
            "true || false",
            "3 instanceof Integer",
    })
    public void testMatchesBinaryExpression(String expression) {
        assertTrue(binaryExpressionType.matches(expression));
    }
}