package org.projectparams.annotationprocessing.astcommons.parsing.expressions.lambda;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;

import java.util.Arrays;

public class LambdaExpressionType extends AbstractExpressionType {
    private static final LambdaExpressionType INSTANCE = new LambdaExpressionType();
    public static LambdaExpressionType getInstance() {
        return INSTANCE;
    }

    private LambdaExpressionType() {
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        if (!matches(expression)) {
            throw new IllegalArgumentException("Invalid lambda expression: " + expression);
        }
        return new LambdaExpression(Arrays.asList(
                expression.substring(1, expression.indexOf(')')).split(",")),
                ExpressionFactory.createExpression(
                        createParams.withExpressionAndNullTag(
                                expression.substring(expression.indexOf('>') + 1).strip()
                        )
                ));
    }

    @Override
    protected boolean matchesInner(String expression) {
        return expression.strip().matches("((\\([\\w\\s,$]*\\))|([\\w$]+))\\s*->\\s*.*");
    }
}
