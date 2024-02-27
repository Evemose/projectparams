package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess.ArrayAccessExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class FieldAccessExpressionType extends AbstractExpressionType {

    private static final FieldAccessExpressionType INSTANCE = new FieldAccessExpressionType();

    private FieldAccessExpressionType() {
    }

    public static FieldAccessExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean matchesInner(String expression) {
        return expression.strip().matches(".*\\.[a-zA-Z_](\\w|\\d|\\.|\\$)*");
    }

    @Override
    protected boolean isCovered(String expression) {
        return CastExpressionType.getInstance().matches(expression)
                || ArrayAccessExpressionType.getInstance().matches(expression)
                || LiteralExpressionType.getInstance().matches(expression);
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
