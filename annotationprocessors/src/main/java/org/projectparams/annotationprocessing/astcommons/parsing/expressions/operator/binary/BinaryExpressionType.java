package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class BinaryExpressionType implements ExpressionType {
    private static final BinaryExpressionType INSTANCE = new BinaryExpressionType();

    private BinaryExpressionType() {}

    public JCTree.Tag extractBinaryOperator(String expression) {
        return extractBinaryOperator(expression, getOperatorIndex(expression));
    }

    private JCTree.Tag extractBinaryOperator(String expression, int operatorIndex) {
        expression = expression.strip();
        var substring = expression.substring(operatorIndex).strip();
        if (substring.startsWith("+")) {
            return JCTree.Tag.PLUS;
        }
        if (substring.startsWith("-")) {
            return JCTree.Tag.MINUS;
        }
        if (substring.startsWith("*")) {
            return JCTree.Tag.MUL;
        }
        if (substring.startsWith("/")) {
            return JCTree.Tag.DIV;
        }
        if (substring.startsWith("%")) {
            return JCTree.Tag.MOD;
        }
        if (substring.startsWith("&&")) {
            return JCTree.Tag.AND;
        }
        if (substring.startsWith("||")) {
            return JCTree.Tag.OR;
        }
        if (substring.startsWith("&")) {
            return JCTree.Tag.BITAND;
        }
        if (substring.startsWith("|")) {
            return JCTree.Tag.BITOR;
        }
        if (substring.startsWith("^")) {
            return JCTree.Tag.BITXOR;
        }
        if (substring.startsWith("<<")) {
            return JCTree.Tag.SL;
        }
        if (substring.startsWith(">>")) {
            return JCTree.Tag.SR;
        }
        if (substring.startsWith(">>>")) {
            return JCTree.Tag.USR;
        }
        if (substring.startsWith("<")) {
            return JCTree.Tag.LT;
        }
        if (substring.startsWith(">")) {
            return JCTree.Tag.GT;
        }
        if (substring.startsWith("<=")) {
            return JCTree.Tag.LE;
        }
        if (substring.startsWith(">=")) {
            return JCTree.Tag.GE;
        }
        if (substring.startsWith("==")) {
            return JCTree.Tag.EQ;
        }
        if (substring.startsWith("!=")) {
            return JCTree.Tag.NE;
        }
        if (substring.startsWith("instanceof")) {
            return JCTree.Tag.TYPETEST;
        }
        throw new IllegalArgumentException("Unknown binary operator in " + expression);
    }

    @Override
    public boolean matches(String expression) {
        return getOperatorIndex(expression) != -1;
    }

    private int getOperatorIndex(String expression) {
        var bracketsCount = new HashMap<>(Map.of('(', 0, '[', 0, '{', 0));
        if (expression.charAt(0) == '(' || expression.charAt(0) == '[' || expression.charAt(0) == '{') {
            bracketsCount.merge(expression.charAt(0), 1, Integer::sum);
        }
        for (var i = 1; i < expression.length()-1; i++) {
            var c = expression.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                bracketsCount.merge(c, 1, Integer::sum);
            } else if (c == ']' || c == '}') {
                bracketsCount.merge((char) (c - 2), -1, Integer::sum);
            } else if (c == ')') {
                // opening bracket has code 40, closing bracket has code 41,
                // while other brackets have offset 2 between opening and closing bracket
                // so this case is exceptional
                bracketsCount.merge('(', -1, Integer::sum);
            } else if (bracketsCount.values().stream().anyMatch(count -> count < 0)) {
                return -1;
            } else if (bracketsCount.values().stream().allMatch(Predicate.isEqual(0))
            && isOperator(expression, i)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isOperator(String expression, int startIndex) {
        var checkedChar = expression.charAt(startIndex);
        return startIndex!=0 && (checkedChar == '*' || checkedChar == '/'
                || checkedChar == '%' || checkedChar == '&'
                || checkedChar == '|' || checkedChar == '^'
                || checkedChar == '='
                || expression.startsWith("instanceof", startIndex)
                || (checkedChar == '-' && expression.charAt(startIndex - 1) != '-' && expression.charAt(startIndex + 1) != '-')
                || (checkedChar == '+' && expression.charAt(startIndex - 1) != '+' && expression.charAt(startIndex + 1) != '+')
                || ((checkedChar == '<' || checkedChar == '>') && !isTypeArgsBracket(expression, startIndex)));
    }

    private boolean isTypeArgsBracket(String expression, int charIndex) {
        String before;
        String after;
        if (expression.charAt(charIndex) == '<') {
            var closeBracketIndex = getCorrespondingTypeArgBracket(expression, charIndex, 1);
            if (closeBracketIndex == -1) {
                return false;
            }
            before = expression.substring(0, charIndex);
            after = expression.substring(closeBracketIndex + 1);
        } else if (expression.charAt(charIndex) == '>') {
            var openBracketIndex = getCorrespondingTypeArgBracket(expression, charIndex, -1);
            if (openBracketIndex == -1) {
                return false;
            }
            before = expression.substring(0, openBracketIndex);
            after = expression.substring(charIndex + 1);
        } else {
            return false;
        }
        return !after.isEmpty() && (before.isEmpty() || before.charAt(before.length() - 1) == '.' || after.charAt(0) == '.'
                || after.charAt(0) == '(' || after.charAt(0) == '[');
    }

    private int getCorrespondingTypeArgBracket(String expression, int fromIndex, int step) {
        var bracketsCount = step;
        for (var i = fromIndex + step; i >= 0 && i < expression.length(); i+=step) {
            if (expression.charAt(i) == '<') {
                bracketsCount++;
                if (bracketsCount == 0) {
                    return i;
                }
            } else if (expression.charAt(i) == '>') {
                bracketsCount--;
                if (bracketsCount == 0) {
                    return i;
                }
            } else if (expression.charAt(i) == ')') {
                // result of comparison is boolean value,
                // and booleans are not comparable with > or < operators
                // so in order to compare result of comparison
                // we have to mutate resulting boolean somehow
                // and to mutate result of operation in any way, it must be enclosed in brackets
                // so if it is enclosed in brackets, it is not a type argument bracket
                // also '(' symbol is prohibited in type arguments brackets
                return -1;
            }
        }
        return -1;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        var operatorIndex = getOperatorIndex(expression);
        if (operatorIndex == -1) {
            throw new IllegalArgumentException("Expression is not a binary expression");
        }
        return new BinaryExpression(
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(0, operatorIndex).strip())),
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(operatorIndex + 1).strip())),
                extractBinaryOperator(expression, operatorIndex)
        );
    }

    public static BinaryExpressionType getInstance() {
        return INSTANCE;
    }
}
