package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary;

import com.sun.tools.javac.tree.JCTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class UnaryExpressionTypeTest {

    private static final UnaryExpressionType unaryExpressionType = UnaryExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {"+5", "-5", "++5", "--5", "!5", "~5", 
                            "5++", "5--", "++var", "--var", 
                            "var++", "var--", "+getMethod()", 
                            "-obj.getMethod(param)", "++arr[3]", 
                            "-myList.get(index)", "!(boolMethod())", 
                            "~object.method().anotherMethod()", "-(method())"})
    void testMatchesWithValidExpressions(String expression) {
        Assertions.assertTrue(unaryExpressionType.matches(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"5**", "5//", "var**", "var//", "+", "-",
                            "++", "--", "!", "~", "**method()", "(+var)",
                            "//object.method", "(*)", "/-(method()++)","(++arr[index])"})
    void testMatchesWithInvalidExpressions(String expression) {
        Assertions.assertFalse(unaryExpressionType.matches(expression));
    }
}