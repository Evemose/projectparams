package org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import static org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory.createExpression;

public class ArrayAccessExpressionType extends AbstractExpressionType {
    private static final ArrayAccessExpressionType INSTANCE = new ArrayAccessExpressionType();
    @Override
    protected boolean matchesInner(String expression) {
        return expression.matches(".*[a-zA-Z_][\\w.$]*\\s*(\\[.*])+$");
    }

    // TODO: add validation for array access expression
    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var openingBracketIndex = ParsingUtils.getArrayIndexStartIndex(expression);
        var array = expression.substring(0, openingBracketIndex);
        var index = expression.substring(openingBracketIndex + 1, expression.length() - 1);
        return new ArrayAccessExpression(
                createExpression(createParams.withExpressionAndNullTag(array)),
                createExpression(createParams.withExpressionAndNullTag(index))
        );
    }

    private ArrayAccessExpressionType() {}

    public static ArrayAccessExpressionType getInstance() {
        return INSTANCE;
    }
}