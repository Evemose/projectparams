package org.projectparams.annotationprocessing.astcommons.parsing.utils;

import org.projectparams.annotationprocessing.astcommons.parsing.ExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess.ArrayAccessExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.ident.IdentifierExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.FieldAccessExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation.MethodInvocationExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass.NewClassExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.newarray.NewArrayExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary.UnaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.parenthezied.ParenthesizedExpressionType;

import java.util.List;
import java.util.stream.IntStream;

public class ExpressionUtils {

    private static final List<ExpressionType> TYPES = List.of(
            IdentifierExpressionType.getInstance(),
            FieldAccessExpressionType.getInstance(),
            NewArrayExpressionType.getInstance(),
            MethodInvocationExpressionType.getInstance(),
            NewClassExpressionType.getInstance(),
            ArrayAccessExpressionType.getInstance(),
            BinaryExpressionType.getInstance(),
            UnaryExpressionType.getInstance(),
            CastExpressionType.getInstance(),
            ParenthesizedExpressionType.getInstance(),
            ConditionalExpressionType.getInstance(),
            LiteralExpressionType.getInstance()
    );

    public static ExpressionType getType(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        var types = TYPES.stream().filter(type -> type.matches(expression)).toList();
        if (types.size() > 1) {
            throw new IllegalStateException("Ambiguous expression type: " + createParams.expression()
            + " matches " + types.stream().map(ExpressionType::getClass).map(Class::getSimpleName).toList());
        }
        if (types.isEmpty()) {
            throw new IllegalStateException("Cannot determine expression type: " + createParams.expression());
        }
        return types.getFirst();
    }

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
