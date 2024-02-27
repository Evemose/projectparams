package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.ident;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary.UnaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;


public class IdentifierExpressionType extends AbstractExpressionType {
    private static final IdentifierExpressionType INSTANCE = new IdentifierExpressionType();

    private IdentifierExpressionType() {
    }

    public static IdentifierExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    protected boolean matchesInner(String expression) {
        expression = expression.strip();
        // that`s faster way to exclude any non-identifiers then calling their type matchers
        // equivalent to:
        // return !ArrayAccessType.getInstance().matches(expression)
        //         && !CastExpressionType.getInstance().matches(expression)
        //         && !MethodInvocationExpressionType.getInstance().matches(expression)
        //         && !NewClassExpressionType.getInstance().matches(expression)
        //         && !NewArrayExpressionType.getInstance().matches(expression)
        //         && !MemberReferenceExpressionType.getInstance().matches(expression)
        return !ParsingUtils.containsTopLevelDot(expression)
                && !expression.endsWith("]")
                && !expression.endsWith(")")
                && !expression.endsWith("}")
                && !expression.startsWith("(")
                && !expression.contains(":");
    }

    @Override
    protected boolean isCovered(String expression) {
        return ConditionalExpressionType.getInstance().matches(expression)
                || BinaryExpressionType.getInstance().matches(expression)
                || UnaryExpressionType.getInstance().matches(expression)
                || LiteralExpressionType.getInstance().matches(expression);
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        if (expression.contains("<") || expression.contains(">")) {
            return new ParametrizedIdentifierExpression(
                    expression.substring(0, ParsingUtils.getTypeArgsStartIndex(expression)).strip(),
                    ExpressionUtils.getTypeArgs(createParams)
            );
        } else {
            return new IdentifierExpression(expression);
        }
    }
}
