package org.projectparams.annotationprocessing.astcommons.parsing.expressions.lambda;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;

import java.util.Arrays;
import java.util.List;

public class LambdaExpressionType extends AbstractExpressionType {
    private static final LambdaExpressionType INSTANCE = new LambdaExpressionType();

    private LambdaExpressionType() {
    }

    public static LambdaExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        if (!matches(expression)) {
            throw new IllegalArgumentException("Invalid lambda expression: " + expression);
        }
        return new LambdaExpression(
                expression.startsWith("(") ? Arrays.stream(
                                expression.substring(1, expression.indexOf(')')).split(","))
                        .filter(s -> !s.isBlank()).toList() :
                        List.of(expression.substring(0, expression.indexOf("->")).strip()),
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
