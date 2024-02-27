package org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.lambda.LambdaExpressionType;

public class CastExpressionType extends AbstractExpressionType {
    private static final CastExpressionType INSTANCE = new CastExpressionType();

    private CastExpressionType() {
    }

    public static CastExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean matchesInner(String expression) {
        return expression.strip().matches("\\(([a-zA-Z_][\\w.$]*(\\s*\\.\\s*)?)+\\).+");
    }

    @Override
    protected boolean isCovered(String expression) {
        return ConditionalExpressionType.getInstance().matches(expression)
                || LambdaExpressionType.getInstance().matches(expression);
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        if (!matches(createParams.expression())) {
            throw new IllegalArgumentException("Invalid cast expression: " + createParams.expression());
        }
        var expression = createParams.expression().strip();
        return new CastExpression(
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(expression.indexOf(')') + 1))),
                expression.substring(expression.indexOf('(') + 1, expression.indexOf(')')));
    }
}
