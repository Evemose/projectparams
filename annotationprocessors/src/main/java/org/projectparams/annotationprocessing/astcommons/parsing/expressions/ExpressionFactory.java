package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess.ArrayAccessExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary.UnaryExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary.UnaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import javax.annotation.processing.Messager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class ExpressionFactory {

    public static Messager messager;

    public enum Type {
        NEW_CLASS(ExpressionFactory::createNewClassExpression),
        METHOD_INVOCATION(ExpressionFactory::createMethodInvocationExpression),
        LITERAL(ExpressionFactory::createLiteralExpression),
        IDENTIFIER(ExpressionFactory::createIdentifierExpression),
        TERNARY(ExpressionFactory::createTernaryExpression),
        PARENTHESIZED(ExpressionFactory::createParenthesizedExpression),
        BINARY(ExpressionFactory::createBinaryExpression),
        UNARY(ExpressionFactory::createUnaryExpression),
        CAST(ExpressionFactory::createCastExpression),
        ARRAY_ACCESS(ExpressionFactory::createArrayAccessExpression),
        NEW_ARRAY(ExpressionFactory::createNewArrayExpression),
        FIELD_ACCESS(ExpressionFactory::createFieldAccessExpression);

        private final Function<CreateExpressionParams, Expression> expressionCreator;

        Type(Function<CreateExpressionParams, Expression> expressionCreator) {
            this.expressionCreator = expressionCreator;
        }

        // order of predicates is crucial
        private static final LinkedHashMap<Predicate<String>, Type> typePredicates = new LinkedHashMap<>() {{
            put(Type::isParenthesized, PARENTHESIZED);
            put(Type::isTernary, TERNARY);
            put(Type::isBinary, BINARY);
            put(Type::isUnary, UNARY);
            put(Type::isCast, CAST);
            put(Type::isNewClass, NEW_CLASS);
            put(Type::isMethodInvocation, METHOD_INVOCATION);
            put(Type::isLiteral, LITERAL);
            put(Type::isNewArray, NEW_ARRAY);
            put(Type::isArrayAccess, ARRAY_ACCESS);
            put(Type::isFieldAccess, FIELD_ACCESS);
            put(s -> true, IDENTIFIER);
        }};

        public static Type of(String expression) {
            return typePredicates.entrySet().stream()
                    .filter(entry -> entry.getKey().test(expression.trim()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown expression type: " + expression.strip()));
        }

        private static boolean isFieldAccess(String expression) {
            return ParsingUtils.containsTopLevelDot(expression);
        }

        private static boolean isMethodInvocation(String expression) {
            return expression.endsWith(")");
        }

        private static boolean isNewArray(String expression) {
            return expression.strip().matches("new\\s+(\\w|\\.)+\\s*(\\[.*])+(\\s*\\{.*})?");
        }

        private static boolean isArrayAccess(String expression) {
            return expression.endsWith("]");
        }

        private static boolean isNewClass(String expression) {
            expression = expression.strip();
            if (!expression.endsWith(")")) {
                return false;
            }
            expression = expression.substring(ParsingUtils.getOwnerSeparatorIndex(expression)+1).strip();
            return expression.matches("^new\\s[^.]*");
        }

        private static boolean isLiteral(String expression) {
            return expression.matches("(\\d+(\\.\\d*)?[fdlFDL]?)|(true|false)|('.')|(\".*\")");
        }

        private static boolean isCast(String expression) {
            return expression.matches("\\((\\w+\\s*(\\.)?)+\\).+");
        }

        private static boolean isUnary(String expression) {
            return expression.matches("(\\+|-|!|~|\\+\\+|--)\\(?.+\\)?")
                    || expression.matches("\\(?.+\\)?(\\+\\+|--)");
        }

        private static boolean isBinary(String expression) {
            return expression.matches(".+(\\+|-|\\*|/|%|&|\\||\\^|<<|>>|>>>|<|>|<=|>=|==|!=|&&|\\|\\||instanceof).+")
                    && !expression.matches(".+<(\\w|\\s|,|>|<)*>((\\w|\\s)+)?(\\(.*\\))?");
        }

        private static boolean isTernary(String expression) {
            return expression.matches("[^(]+\\?.+:.+");
        }

        private static boolean isParenthesized(String expression) {
            return expression.matches("\\(.+\\)") && !isCast(expression);
        }
    }

    private ExpressionFactory() {
    }

    private static Expression createNewArrayExpression(CreateExpressionParams createExpression) {
        var expression = createExpression.expression();
        var type = expression.substring(4, expression.indexOf('[')).strip();
        var initializerStartIndex = expression.indexOf('{');
        var dimensions = getDimensions(createExpression, initializerStartIndex);
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

    private static List<Expression> getDimensions(CreateExpressionParams createExpression, int initializerStartIndex) {
        var expression = createExpression.expression();
        if (initializerStartIndex != -1) {
            return IntStream.range(0, (int) expression.chars().limit(expression.indexOf('{')).filter(ch -> ch == '[').count())
                    .<Expression>mapToObj(i -> null)
                    .toList();
        }
        var dimensionsString = expression.substring(
                expression.indexOf('[', ParsingUtils.getArgsStartIndex(expression) + 1),
                        expression.lastIndexOf(']') + 1).strip();
        return ParsingUtils.getArrayDimensions(dimensionsString)
                .stream()
                .map(dim -> createExpression(createExpression.withExpressionAndNullTag(dim))).toList();
    }

    private static Expression createArrayAccessExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var openingBracketIndex = ParsingUtils.getArrayIndexStartIndex(expression);
        var array = expression.substring(0, openingBracketIndex);
        var index = expression.substring(openingBracketIndex + 1, expression.length() - 1);
        return new ArrayAccessExpression(
                createExpression(createParams.withExpressionAndNullTag(array)),
                createExpression(createParams.withExpressionAndNullTag(index))
        );
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Expression createLiteralExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression().trim();
        var typeTag = createParams.typeTag();
        if (typeTag == null) {
            typeTag = TypeUtils.geLiteralTypeTag(expression);
        }
        var value = TypeUtils.literalValueFromStr(typeTag, expression);
        return new LiteralExpression(value, value.getClass());
    }

    private static Expression createMethodInvocationExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var argsStartIndex = ParsingUtils.getArgsStartIndex(expression);
        var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
        return new MethodInvocationExpression(expression.substring(
                Math.max(ownerSeparatorIndex, expression.lastIndexOf('>', argsStartIndex)) + 1, argsStartIndex).strip(),
                getOwner(createParams),
                ParsingUtils.getArgStrings(expression, '(', ')').stream().map(arg -> createExpression(createParams.withExpressionAndNullTag(arg))).toList(),
                    createParams.parsingContextPath(),
                getTypeParameters(expression).stream().map(typeArg ->
                            createExpression(createParams.withExpressionAndNullTag(typeArg))).toList());
    }

    private static ArrayList<String> getTypeParameters(String expression) {
        var typeParameters = new ArrayList<String>();
        var typeArgsEndIndex = expression.lastIndexOf('>');
        if (typeArgsEndIndex != -1) {
            var typeArgsParts = ParsingUtils.getTypeArgStrings(expression);
            for (var typeArg : typeArgsParts) {
                typeParameters.add(typeArg.strip());
            }
        }
        return typeParameters;
    }

    private static Expression getOwner(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
        return ownerSeparatorIndex == -1 ? null : createExpression(createParams.withExpressionAndNullTag(
                expression.substring(0, ownerSeparatorIndex)));
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

    private static Expression createNewClassExpression(CreateExpressionParams createParams) {
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
                getOwner(createParams),
                ParsingUtils.getArgStrings(expression, '(', ')').stream().map(arg -> createExpression(createParams.withExpressionAndNullTag(arg))).toList(),
                createParams.parsingContextPath(),
                getTypeParameters(expression).stream().map(typeArg ->
                        createExpression(createParams.withExpressionAndNullTag(typeArg))).toList());
    }

    private static Expression createTernaryExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var questionMarkIndex = expression.indexOf('?');
        var colonIndex = expression.indexOf(':');
        return new TernaryExpression(
                createExpression(createParams.withExpressionAndNullTag(expression.substring(0, questionMarkIndex))),
                createExpression(createParams.withExpressionAndNullTag(expression.substring(questionMarkIndex + 1, colonIndex))),
                createExpression(createParams.withExpressionAndNullTag(expression.substring(colonIndex + 1)))
        );
    }

    private static Expression createParenthesizedExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        return new ParenthesizedExpression(
                createExpression(createParams.withExpressionAndNullTag(expression.substring(1, expression.length() - 1)))
        );
    }

    private static Expression createCastExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        return new CastExpression(
                createExpression(createParams
                        .withExpressionAndNullTag(expression.substring(expression.indexOf(')') + 1))),
                expression.substring(expression.indexOf('(') + 1, expression.indexOf(')')));
    }

    private static Expression createUnaryExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var operator = UnaryExpressionType.extractUnaryOperator(expression);
        String operand;
        if (operator == JCTree.Tag.POSTDEC || operator == JCTree.Tag.POSTINC) {
            operand = expression.substring(0, expression.length() - 2);
        } else {
            operand = expression.substring(expression.indexOf(ParsingUtils.getStringOfOperator(operator)) + ParsingUtils.getStringOfOperator(operator).length());
        }
        return new UnaryExpression(
                createExpression(createParams.withExpressionAndNullTag(operand)),
                operator
        );
    }

    private static Expression createBinaryExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var operator = BinaryExpressionType.getInstance().extractBinaryOperator(expression);
        var firstOperandEnd = ParsingUtils.getClosingArgsParenthesesIndex(expression, 0);
        if (firstOperandEnd == -1) {
            firstOperandEnd = expression.indexOf(ParsingUtils.getStringOfOperator(operator));
        }
        return new BinaryExpression(
                createExpression(createParams.withExpressionAndNullTag(expression.substring(0, firstOperandEnd + 1))),
                createExpression(createParams.withExpressionAndNullTag(
                        expression.substring(expression.indexOf(ParsingUtils.getStringOfOperator(operator),
                                firstOperandEnd + 1) + ParsingUtils.getStringOfOperator(operator).length()))),
                operator);
    }

    public static Expression createExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        //messager.printMessage(Diagnostic.Kind.NOTE, "Creating expression from " + expression);
        if (expression == null) {
            return LiteralExpression.NULL;
        }
        expression = expression.trim();
        var type = Type.of(expression);
        //messager.printMessage(Diagnostic.Kind.NOTE, "Type of expression: " + type);
        return type.expressionCreator.apply(createParams);
    }
}
