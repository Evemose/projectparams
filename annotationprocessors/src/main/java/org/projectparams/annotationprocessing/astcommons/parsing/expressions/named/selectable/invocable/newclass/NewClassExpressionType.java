package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.*;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class NewClassExpressionType implements ExpressionType {
    private static final NewClassExpressionType INSTANCE = new NewClassExpressionType();
    @Override
    public boolean matches(String expression) {
        expression = expression.strip();
        if (!expression.endsWith(")")) {
            return false;
        }
        expression = expression.substring(ParsingUtils.getOwnerSeparatorIndex(expression)+1).strip();
        return expression.matches("^new\\s+\\w(\\w|(\\s*\\.(?!(\\s*\\W))\\s*)|\\d)+" +
                "(\\s*<\\s*(\\w|(\\s*\\.(?!(\\s*\\W))\\s*)|(\\s*,(?!(\\s*\\W))\\s*)|\\d)*>)?\\s*\\(.*\\)$");
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
