package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class NewClassExpressionType extends AbstractExpressionType {
    private static final NewClassExpressionType INSTANCE = new NewClassExpressionType();
    @Override
    protected boolean matchesInner(String expression) {
        expression = expression.strip();
        if (!expression.endsWith(")")) {
            return false;
        }
        return !ParsingUtils.containsTopLevelDot(expression) && expression.matches("new\\s.+")
                || expression.substring(ParsingUtils.getOwnerSeparatorIndex(expression) + 1)
                .strip().matches("new\\s.+");
    }

    @Override
    protected boolean isCovered(String expression) {
        return ConditionalExpressionType.getInstance().matches(expression);
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var rightBound = ParsingUtils.getTypeArgsStartIndex(expression);
        if (rightBound == -1) {
            rightBound = ParsingUtils.getArgsStartIndex(expression);
            if (rightBound == -1) {
                rightBound = expression.length();
            }
        }
        return new NewClassExpression(
                expression.substring(ParsingUtils.getSelectedNewKeywordIndex(expression) + 3, rightBound).strip(),
                ExpressionUtils.getOwner(createParams),
                ExpressionUtils.getArgs(createParams),
                createParams.parsingContextPath(),
                ExpressionUtils.getTypeArgs(createParams));
    }

    private NewClassExpressionType() {}

    public static NewClassExpressionType getInstance() {
        return INSTANCE;
    }
}
