package org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionType;

public class CastExpressionType implements ExpressionType {
    private static final CastExpressionType INSTANCE = new CastExpressionType();
    @Override
    public boolean matches(String expression) {
        expression = expression.strip();
        return expression.matches("\\((\\w(\\w|\\d)+(\\s*\\.\\s*)?)+\\).+");
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

    private CastExpressionType() {}

    public static CastExpressionType getInstance() {
        return INSTANCE;
    }
}
