package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass.NewClassExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.parenthezied.ParenthesizedExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class MethodInvocationExpressionType extends AbstractExpressionType {

    private static final MethodInvocationExpressionType INSTANCE = new MethodInvocationExpressionType();
    @Override
    public boolean matchesInner(String expression) {
        return expression.endsWith(")");
    }

    @Override
    public boolean isCovered(String expression) {
        return  ParenthesizedExpressionType.getInstance().matches(expression)
                || CastExpressionType.getInstance().matches(expression)
                || NewClassExpressionType.getInstance().matches(expression);
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var argsStartIndex = ParsingUtils.getArgsStartIndex(expression);
        var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
        return new MethodInvocationExpression(
                expression.substring(Math.max(ownerSeparatorIndex,
                        expression.lastIndexOf('>', argsStartIndex)) + 1, argsStartIndex).strip(),
                ExpressionUtils.getOwner(createParams),
                ExpressionUtils.getArgs(createParams),
                createParams.parsingContextPath(),
                ExpressionUtils.getTypeArgs(createParams));
    }

    private MethodInvocationExpressionType() {}

    public static MethodInvocationExpressionType getInstance() {
        return INSTANCE;
    }
}
