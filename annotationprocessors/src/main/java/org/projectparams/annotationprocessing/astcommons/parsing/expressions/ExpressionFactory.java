package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess.ArrayAccessType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.ident.ParametrizedIdentifierExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.FieldAccessExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.ident.IdentifierExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.ParametrizedFieldAccessExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation.MethodInvocationExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass.NewClassExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.FieldAccessExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary.UnaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.parenthezied.ParenthesizedExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public class ExpressionFactory {

    public static Messager messager;

    public enum Type {
        NEW_CLASS(NewClassExpressionType.getInstance()::parse),
        METHOD_INVOCATION(MethodInvocationExpressionType.getInstance()::parse),
        LITERAL(ExpressionFactory::createLiteralExpression),
        IDENTIFIER(ExpressionFactory::createIdentifierExpression),
        TERNARY(ConditionalExpressionType.getInstance()::parse),
        PARENTHESIZED(ParenthesizedExpressionType.getInstance()::parse),
        BINARY(BinaryExpressionType.getInstance()::parse),
        UNARY(UnaryExpressionType.getInstance()::parse),
        CAST(CastExpressionType.getInstance()::parse),
        ARRAY_ACCESS(ArrayAccessType.getInstance()::parse),
        NEW_ARRAY(ExpressionFactory::createNewArrayExpression),
        FIELD_ACCESS(ExpressionFactory::createFieldAccessExpression);

        private final Function<CreateExpressionParams, Expression> expressionCreator;

        Type(Function<CreateExpressionParams, Expression> expressionCreator) {
            this.expressionCreator = expressionCreator;
        }

        // order of predicates is crucial
        private static final LinkedHashMap<Predicate<String>, Type> typePredicates = new LinkedHashMap<>() {{
            put(ParenthesizedExpressionType.getInstance()::matches, PARENTHESIZED);
            put(ConditionalExpressionType.getInstance()::matches, TERNARY);
            put(BinaryExpressionType.getInstance()::matches, BINARY);
            put(UnaryExpressionType.getInstance()::matches, UNARY);
            put(CastExpressionType.getInstance()::matches, CAST);
            put(NewClassExpressionType.getInstance()::matches, NEW_CLASS);
            put(MethodInvocationExpressionType.getInstance()::matches, METHOD_INVOCATION);
            put(Type::isLiteral, LITERAL);
            put(Type::isNewArray, NEW_ARRAY);
            put(ArrayAccessType.getInstance()::matches, ARRAY_ACCESS);
            put(FieldAccessExpressionType.getInstance()::matches, FIELD_ACCESS);
            put(s -> true, IDENTIFIER);
        }};

        public static Type of(String expression) {
            return typePredicates.entrySet().stream()
                    .filter(entry -> entry.getKey().test(expression.trim()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown expression type: " + expression.strip()));
        }

        private static boolean isNewArray(String expression) {
            return expression.strip().matches("new\\s+(\\w|\\.)+\\s*(\\[.*])+(\\s*\\{.*})?");
        }

        private static boolean isLiteral(String expression) {
            return expression.matches("(\\d+(\\.\\d*)?[fdlFDL]?)|(true|false)|('.')|(\".*\")");
        }
    }

    private ExpressionFactory() {
    }

    private static Expression createNewArrayExpression(CreateExpressionParams createExpression) {
        var expression = createExpression.expression();
        var type = expression.substring(4, expression.indexOf('[')).strip();
        var initializerStartIndex = expression.indexOf('{');
        var dimensions = ExpressionUtils.getDimensions(createExpression, initializerStartIndex);
        var initializer = getInitializer(createExpression, initializerStartIndex, dimensions.size(), type);
        return new NewArrayExpression(type, dimensions, initializer);
    }

    private static List<Expression> getInitializer(CreateExpressionParams createExpression,
                                                   int initializerStartIndex,
                                                   int dimsCount,
                                                   String type) {
        List<Expression> initializer = null;
        if (initializerStartIndex != -1) {
            initializer = ParsingUtils.getArrayInitializerExpressions(createExpression.expression())
                    .stream()
                    .map(init -> {
                        if (init.trim().matches("\\{.*}")) {
                            var arrayDims = dimsCount - 1;
                            if (arrayDims < 1) {
                                throw new IllegalArgumentException("Array initializer contains too many dimensions: "
                                        + init + " in " + createExpression.expression());
                            }
                            return createExpression(createExpression.withExpressionAndNullTag(
                                    "new %s".formatted(type) + "[]".repeat(dimsCount-1) + init));
                        }
                        return createExpression(createExpression.withExpressionAndNullTag(init));
                    }).toList();
        }
        return initializer;
    }

    private static Expression createIdentifierExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        if (expression.contains("<") || expression.contains(">")) {
            var typeArgsStartIndex = ParsingUtils.getTypeArgsStartIndex(expression);
            var name = expression.substring(0, typeArgsStartIndex);
            var typeArgs = ParsingUtils.getTypeArgStrings(expression);
            return new ParametrizedIdentifierExpression(
                    name.strip(),
                    typeArgs.stream().map(arg ->
                            createExpression(createParams.withExpressionAndNullTag(arg))).toList()
            );
        } else {
            return new IdentifierExpression(expression);
        }
    }

    private static Expression createLiteralExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression().trim();
        var typeTag = createParams.typeTag();
        if (typeTag == null) {
            typeTag = TypeUtils.geLiteralTypeTag(expression);
        }
        var value = TypeUtils.literalValueFromStr(typeTag, expression);
        return new LiteralExpression(value);
    }

    private static Expression createFieldAccessExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
        var owner = expression.substring(0, ownerSeparatorIndex);
        if (expression.substring(ownerSeparatorIndex + 1).contains("<")) {
            return new ParametrizedFieldAccessExpression(
                    expression.substring(ownerSeparatorIndex + 1, expression.indexOf('<')),
                    createExpression(createParams.withExpressionAndNullTag(owner)),
                    ParsingUtils.getTypeArgStrings(expression).stream().map(typeArg ->
                            createExpression(createParams.withExpressionAndNullTag(typeArg))).toList());
        } else {
            return new FieldAccessExpression(expression.substring(ownerSeparatorIndex + 1),
                    createExpression(createParams.withExpressionAndNullTag(owner)));
        }
    }

    public static Expression createExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        messager.printMessage(Diagnostic.Kind.NOTE, "Creating expression from " + expression);
        if (expression == null) {
            return LiteralExpression.NULL;
        }
        expression = expression.trim();
        var type = Type.of(expression);
        messager.printMessage(Diagnostic.Kind.NOTE, "Type of expression: " + type);
        return type.expressionCreator.apply(createParams);
    }
}
