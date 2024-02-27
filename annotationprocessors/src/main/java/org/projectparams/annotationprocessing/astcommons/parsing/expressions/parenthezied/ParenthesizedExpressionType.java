package org.projectparams.annotationprocessing.astcommons.parsing.expressions.parenthezied;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class ParenthesizedExpressionType extends AbstractExpressionType {
    private static final ParenthesizedExpressionType INSTANCE = new ParenthesizedExpressionType();

    public static ParenthesizedExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean matchesInner(String expression) {
        expression = expression.strip();
        return expression.startsWith("(")
                && expression.endsWith(")")
                && ParsingUtils.getCorrespondingOpeningParIndex(expression, '(', ')', expression.length() - 1) == 0;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        if (!matches(createParams.expression())) {
            throw new IllegalArgumentException("Invalid parenthesized expression: " + createParams.expression());
        }
        var expression = createParams.expression().strip();
        return new ParenthesizedExpression(
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(1, expression.length() - 1)))
        );
    }
}
