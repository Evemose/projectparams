package org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class ConditionalExpressionType extends AbstractExpressionType {
    private static final ConditionalExpressionType INSTANCE = new ConditionalExpressionType();
    @Override
    public boolean matchesInner(String expression) {
        return ParsingUtils.countMatchingTopLevelSymbols(expression, ParsingUtils.equalsSymbolPredicate('?')) == 1
                && ParsingUtils.countMatchingTopLevelSymbols(expression, ParsingUtils.equalsSymbolPredicate(':')) == 1;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var questionMarkIndex = ParsingUtils.getMatchingTopLevelSymbolIndex(expression, ParsingUtils.equalsSymbolPredicate('?'));
        var colonIndex = ParsingUtils.getMatchingTopLevelSymbolIndex(expression, ParsingUtils.equalsSymbolPredicate(':'));
        if (questionMarkIndex == -1 || colonIndex == -1
                || ParsingUtils.getMatchingTopLevelSymbolIndex(expression, ParsingUtils.equalsSymbolPredicate(':'), colonIndex+1) != -1) {
            throw new IllegalArgumentException("Invalid conditional expression: " + expression);
        }
        return new ConditionalExpression(
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(0, questionMarkIndex))),
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(questionMarkIndex + 1, colonIndex))),
                ExpressionFactory.createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(colonIndex + 1)))
        );
    }

    public static ConditionalExpressionType getInstance() {
        return INSTANCE;
    }
}
