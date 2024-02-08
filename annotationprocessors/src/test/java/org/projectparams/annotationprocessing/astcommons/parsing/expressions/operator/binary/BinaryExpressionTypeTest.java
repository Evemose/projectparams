package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary;

import com.sun.tools.javac.tree.JCTree;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;

import javax.annotation.processing.Messager;

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