package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class UnaryExpressionType implements ExpressionType {
    private static final UnaryExpressionType INSTANCE = new UnaryExpressionType();
    private UnaryExpressionType() {}

    public static JCTree.Tag extractUnaryOperator(String expression) {
        expression = expression.strip();
        if (expression.startsWith("++")) {
            return JCTree.Tag.PREINC;
        }
        if (expression.startsWith("--")) {
            return JCTree.Tag.PREDEC;
        }
        if (expression.startsWith("+")) {
            return JCTree.Tag.POS;
        }
        if (expression.startsWith("-")) {
            return JCTree.Tag.NEG;
        }
        if (expression.startsWith("!")) {
            return JCTree.Tag.NOT;
        }
        if (expression.startsWith("~")) {
            return JCTree.Tag.COMPL;
        }
        if (expression.endsWith("++")) {
            return JCTree.Tag.POSTINC;
        }
        if (expression.endsWith("--")) {
            return JCTree.Tag.POSTDEC;
        }
        throw new IllegalArgumentException("Unknown unary operator in " + expression);
    }

    @Override
    public boolean matches(String expression) {
        return expression.matches("(\\+|-|!|~|\\+\\+|--)\\s*[^+-].*")
                || expression.matches(".+(\\+\\+|--)");
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var operator = extractUnaryOperator(expression);
        var operand = ParsingUtils.getStringOfOperator(operator).strip();
        return new UnaryExpression(ExpressionFactory.createExpression(createParams
                .withExpressionAndNullTag(operand)), operator);
    }

    public static UnaryExpressionType getInstance() {
        return INSTANCE;
    }
}
