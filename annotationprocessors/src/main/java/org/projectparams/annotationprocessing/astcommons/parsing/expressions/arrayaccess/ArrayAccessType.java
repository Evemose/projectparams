package org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import static org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory.createExpression;

public class ArrayAccessType implements ExpressionType {
    private static final ArrayAccessType INSTANCE = new ArrayAccessType();
    @Override
    public boolean matches(String expression) {
        return expression.matches(".*\\w+(\\[.*])+$");
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

    private ArrayAccessType() {}

    public static ArrayAccessType getInstance() {
        return INSTANCE;
    }
}
