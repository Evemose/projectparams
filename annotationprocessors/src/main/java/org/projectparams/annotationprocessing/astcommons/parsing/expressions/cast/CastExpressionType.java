package org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;

public class CastExpressionType extends AbstractExpressionType {
    private static final CastExpressionType INSTANCE = new CastExpressionType();
    @Override
    public boolean matchesInner(String expression) {
        return expression.strip().matches("\\((\\w(\\w|\\d)+(\\s*\\.\\s*)?)+\\).+");
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
