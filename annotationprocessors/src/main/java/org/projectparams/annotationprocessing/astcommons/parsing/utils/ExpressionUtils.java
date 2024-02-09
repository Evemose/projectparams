package org.projectparams.annotationprocessing.astcommons.parsing.utils;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;

import java.util.List;
import java.util.stream.IntStream;

public class ExpressionUtils {

    public static Expression getOwner(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
        return ownerSeparatorIndex == -1 ? null : ExpressionFactory.createExpression(createParams.withExpressionAndNullTag(
                expression.substring(0, ownerSeparatorIndex)));
    }

    public static List<Expression> getTypeArgs(CreateExpressionParams createParams) {
        return ParsingUtils.getTypeParameters(createParams.expression()).stream().map(typeArg ->
                ExpressionFactory.createExpression(createParams.withExpressionAndNullTag(typeArg))).toList();
    }

    public static List<Expression> getArgs(CreateExpressionParams createParams) {
        return ParsingUtils.getArgStrings(createParams.expression(), '(', ')').stream()
                .map(arg -> ExpressionFactory.createExpression(createParams.withExpressionAndNullTag(arg))).toList();
    }

    public static List<Expression> getDimensions(CreateExpressionParams createExpression, int initializerStartIndex) {
        var expression = createExpression.expression();
        if (initializerStartIndex != -1) {
            return IntStream.range(0, (int) expression.chars().limit(expression.indexOf('{')).filter(ch -> ch == '[').count())
                    .<Expression>mapToObj(i -> null)
                    .toList();
        }
        var dimensionsString = expression.substring(
                expression.indexOf('[', ParsingUtils.getArgsStartIndex(expression) + 1) + 1,
                expression.lastIndexOf(']') + 1).strip();
        return ParsingUtils.getArrayDimensions(dimensionsString)
                .stream()
                .map(dim -> ExpressionFactory.createExpression(createExpression.withExpressionAndNullTag(dim))).toList();
    }
}
