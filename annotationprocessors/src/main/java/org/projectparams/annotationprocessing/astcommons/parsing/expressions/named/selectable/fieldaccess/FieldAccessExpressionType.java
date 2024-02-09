package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess.ArrayAccessType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class FieldAccessExpressionType extends AbstractExpressionType {

    private static final FieldAccessExpressionType INSTANCE = new FieldAccessExpressionType();

    public static FieldAccessExpressionType getInstance() {
        return INSTANCE;
    }

    private FieldAccessExpressionType() {
    }

    @Override
    public boolean matchesInner(String expression) {
        expression = expression.strip();
        return ParsingUtils.containsTopLevelDot(expression);
    }

    @Override
    protected boolean isCovered(String expression) {
        return expression.endsWith(")")
                || CastExpressionType.getInstance().matches(expression)
                || ArrayAccessType.getInstance().matches(expression);
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
        var owner = expression.substring(0, ownerSeparatorIndex);
        if (expression.substring(ownerSeparatorIndex + 1).contains("<")) {
            return new ParametrizedFieldAccessExpression(
                    expression.substring(ownerSeparatorIndex + 1, expression.indexOf('<')),
                    ExpressionFactory.createExpression(createParams.withExpressionAndNullTag(owner)),
                    ExpressionUtils.getTypeArgs(createParams)
            );
        } else {
            return new FieldAccessExpression(expression.substring(ownerSeparatorIndex + 1),
                    ExpressionFactory.createExpression(createParams.withExpressionAndNullTag(owner)));
        }
    }
}
