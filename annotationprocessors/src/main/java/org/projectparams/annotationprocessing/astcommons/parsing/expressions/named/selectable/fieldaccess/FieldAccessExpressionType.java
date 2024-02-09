package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess.ArrayAccessType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class FieldAccessExpressionType implements ExpressionType {
    @Override
    public boolean matches(String expression) {
        expression = expression.strip();
        return ParsingUtils.containsTopLevelDot(expression)
                && !expression.endsWith(")")
                && !CastExpressionType.getInstance().matches(expression)
                && !ArrayAccessType.getInstance().matches(expression);
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        return null;
    }
}
