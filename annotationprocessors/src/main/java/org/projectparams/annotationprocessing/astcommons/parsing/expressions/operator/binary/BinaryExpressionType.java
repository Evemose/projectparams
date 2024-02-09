package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class BinaryExpressionType implements ExpressionType {
    private static final BinaryExpressionType INSTANCE = new BinaryExpressionType();

    private BinaryExpressionType() {}

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
        return ParsingUtils.getMatchingTopLevelSymbolIndex(expression, this::isOperator) != -1;
    }

    private boolean isOperator(String expression, int startIndex) {
        var checkedChar = expression.charAt(startIndex);
        return startIndex != 0 && startIndex != expression.length()-1 && (checkedChar == '*' || checkedChar == '/'
                || checkedChar == '%' || checkedChar == '&'
                || checkedChar == '|' || checkedChar == '^'
                || checkedChar == '='
                || expression.startsWith("instanceof", startIndex)
                || (checkedChar == '-' && expression.charAt(startIndex - 1) != '-' && expression.charAt(startIndex + 1) != '-')
                || (checkedChar == '+' && expression.charAt(startIndex - 1) != '+' && expression.charAt(startIndex + 1) != '+')
                || ((checkedChar == '<' || checkedChar == '>') && !ParsingUtils.isTypeArgsBracket(expression, startIndex)));
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        var operatorIndex = ParsingUtils.getMatchingTopLevelSymbolIndex(expression, this::isOperator);
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
