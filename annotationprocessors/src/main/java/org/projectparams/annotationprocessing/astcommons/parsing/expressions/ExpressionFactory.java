package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// TODO: implement type casting
@SuppressWarnings("unused")
public class ExpressionFactory {
    public static Messager messager;
    public enum Type {
        NEW_CLASS(createNewClassExpression()),
        METHOD_INVOCATION(createMethodInvocationExpression()),
        LITERAL(createLiteralExpression()),
        IDENTIFIER(createIdentifierExpression()),
        TERNARY(createTernaryExpression()),
        PARENTHESIZED(createParenthesizedExpression()),
        BINARY(createBinaryExpression()),
        UNARY(createUnaryExpression()),
        CAST(createCastExpression()),
        FIELD_ACCESS(createFieldAccessExpression());

        private final Function<CreateExpressionParams, Expression> expressionCreator;

        Type(Function<CreateExpressionParams, Expression> expressionCreator) {
            this.expressionCreator = expressionCreator;
        }

        public static Type of(String expression) {
            expression = expression.strip();
            if (expression.matches("\\(.+\\)") && !expression.matches("\\((\\w+\\s*(\\.)?)+\\).+")) {
                return PARENTHESIZED;
            }
            if (expression.matches("[^(]+\\?.+:.+")) {
                return TERNARY;
            }
            if (expression.matches(".+(\\+|-|\\*|/|%|&|\\||\\^|<<|>>|>>>|<|>|<=|>=|==|!=|&&|\\|\\||instanceof).+")
            && !expression.matches(".+<(\\w|\\s|,|>|<)*>((\\w|\\s)+)?(\\(.*\\))?")) {
                return BINARY;
            }
            if (expression.matches("(\\+|-|!|~|\\+\\+|--)\\(?.+\\)?")
                    || expression.matches("\\(?.+\\)?(\\+\\+|--)")) {
                return UNARY;
            }
            if (expression.matches("\\((\\w+\\s*(\\.)?)+\\).+")) {
                return CAST;
            }
            if (expression.endsWith(")")) {
                var argsStartIndex = ParsingUtils.getArgsStartIndex(expression);
                expression = expression.substring(0, argsStartIndex);
                expression = expression.substring(expression.lastIndexOf('.') + 1, expression.length()-1);
                if (expression.startsWith("new ")) {
                    return NEW_CLASS;
                }
                return METHOD_INVOCATION;
            }
            if (expression.matches("(\\d+(\\.\\d+)?[fdlFDL]?)|(true|false)|('.')|(\".*\")")) {
                return LITERAL;
            }
            if (ParsingUtils.containsTopLevelDot(expression)) {
                return FIELD_ACCESS;
            }
            return IDENTIFIER;
        }
    }

    private ExpressionFactory() {
    }

    /**
     * @param expression The string representation of the expression
     * @param typeTag The type tag of the expression
     * @param parsingContextPath The path to the enclosing invocable element (method, constructor, etc.)
     */
    public record CreateExpressionParams(String expression,
                                         @Nullable TypeTag typeTag,
                                         TreePath parsingContextPath) {
        public CreateExpressionParams withExpression(String expression) {
            return new CreateExpressionParams(expression, typeTag, parsingContextPath);
        }
        // name seems odd, but it`s needed to recursively create expressions
        public CreateExpressionParams withExpressionAndNullTag(String expression) {
            return new CreateExpressionParams(expression, null, parsingContextPath);
        }
    }

    public static class CreateExpressionParamsBuilder {
        private String expression;
        private TypeTag typeTag = null;
        private TreePath parsingContextPath;

        public CreateExpressionParamsBuilder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public CreateExpressionParamsBuilder typeTag(TypeTag typeTag) {
            this.typeTag = typeTag;
            return this;
        }

        public CreateExpressionParamsBuilder parsingContextPath(TreePath parsingContextPath) {
            this.parsingContextPath = parsingContextPath;
            return this;
        }

        public CreateExpressionParams build() {
            if (expression == null) {
                throw new IllegalStateException("Expression cannot be null");
            }
            if (parsingContextPath == null) {
                throw new IllegalStateException("Enclosing invocable path cannot be null");
            }
            return new CreateExpressionParams(expression, typeTag, parsingContextPath);
        }
    }

    private static Function<CreateExpressionParams, Expression> createIdentifierExpression() {
        return createParams -> {
            var expression = createParams.expression();
            if (expression.contains("<") || expression.contains(">")) {
                var typeArgsStartIndex = ParsingUtils.getTypeArgsStartIndex(expression);
                var name = expression.substring(0, typeArgsStartIndex);
                var typeArgs = ParsingUtils.getTypeArgStrings(expression);
                return new ParametrizedIdentifierExpression
                        (name.strip(),
                        typeArgs.stream().map(arg ->
                                createExpression(createParams.withExpressionAndNullTag(arg))).toList());
            } else {
                return new IdentifierExpression(expression);
            }
        };
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Function<CreateExpressionParams, Expression> createLiteralExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var typeTag = createParams.typeTag();
            if (typeTag == null) {
                typeTag = TypeUtils.geLiteralTypeTag(expression);
            }
            var value = TypeUtils.literalValueFromStr(typeTag, expression);
            return new LiteralExpression(value, value.getClass());
        };
    }

    private static Function<CreateExpressionParams, Expression> createMethodInvocationExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var argsStartIndex = ParsingUtils.getArgsStartIndex(expression);
            var args = ParsingUtils.getArgStrings(expression, '(', ')');
            var typeArgsEndIndex = expression.lastIndexOf('>', argsStartIndex);
            var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
            var name = expression.substring(
                    Math.max(ownerSeparatorIndex, typeArgsEndIndex) + 1, argsStartIndex);
            Expression owner = null;
            if (ownerSeparatorIndex != -1) {
                var ownerExpression = expression.substring(0, ownerSeparatorIndex);
                owner = createExpression(createParams.withExpressionAndNullTag(ownerExpression));
            }
            List<String> typeParameters = new ArrayList<>();
            if (typeArgsEndIndex != -1) {
                var typeArgs = expression.substring(ParsingUtils.getTypeArgsStartIndex(expression) + 1,
                        typeArgsEndIndex);
                var typeArgsParts = ParsingUtils.getTypeArgStrings(expression);
                for (var typeArg : typeArgsParts) {
                    typeParameters.add(typeArg.strip());
                }
            }
            return new MethodInvocationExpression(name.strip(),
                    owner,
                    args.stream().map(arg -> createExpression(createParams.withExpressionAndNullTag(arg))).toList(),
                    createParams.parsingContextPath(),
                    typeParameters.stream().map(typeArg ->
                            createExpression(createParams.withExpressionAndNullTag(typeArg))).toList());
        };
    }

    private static Function<CreateExpressionParams, Expression> createFieldAccessExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
            var owner = expression.substring(0, ownerSeparatorIndex);
            if (expression.substring(ownerSeparatorIndex + 1).contains("<")) {
                var accessedField = expression.substring(ownerSeparatorIndex + 1, expression.indexOf('<'));
                var typeArgs = ParsingUtils.getTypeArgStrings(expression);
                return new ParametrizedFieldAccessExpression(accessedField,
                        createExpression(createParams.withExpressionAndNullTag(owner)),
                        typeArgs.stream().map(typeArg ->
                                createExpression(createParams.withExpressionAndNullTag(typeArg))).toList());
            } else {
                var accessedField = expression.substring(ownerSeparatorIndex + 1);
                return new FieldAccessExpression(accessedField, createExpression(createParams.withExpressionAndNullTag(owner)));
            }
        };
    }

    private static Function<CreateExpressionParams, Expression> createNewClassExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var argsStartIndex = ParsingUtils.getArgsStartIndex(expression);
            var args = ParsingUtils.getArgStrings(expression, '(', ')');
            var typeArgsStartIndex = ParsingUtils.getTypeArgsStartIndex(expression);
            if (typeArgsStartIndex == -1) {
                typeArgsStartIndex = expression.length();
            }
            var name = expression.substring(expression.lastIndexOf("new ", argsStartIndex) + 4,
                    Math.min(typeArgsStartIndex, argsStartIndex));
            var ownerSeparatorIndex = ParsingUtils.getOwnerSeparatorIndex(expression);
            Expression owner = null;
            if (ownerSeparatorIndex != -1) {
                var ownerExpression = expression.substring(0, ownerSeparatorIndex);
                owner = createExpression(createParams.withExpressionAndNullTag(ownerExpression));
            }
            List<String> typeParameters = new ArrayList<>();
            if (typeArgsStartIndex != expression.length()) {
                var typeArgs = expression.substring(typeArgsStartIndex + 1,
                        expression.lastIndexOf('>', argsStartIndex));
                var typeArgsParts = ParsingUtils.getTypeArgStrings(expression);
                for (var typeArg : typeArgsParts) {
                    typeParameters.add(typeArg.strip());
                }
            }
            return new NewClassExpression(name,
                    owner,
                    args.stream().map(arg -> createExpression(createParams.withExpressionAndNullTag(arg))).toList(),
                    createParams.parsingContextPath(),
                    typeParameters.stream().map(typeArg ->
                            createExpression(createParams.withExpressionAndNullTag(typeArg))).toList());
        };
    }

    private static Function<CreateExpressionParams, Expression> createTernaryExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var questionMarkIndex = expression.indexOf('?');
            var colonIndex = expression.indexOf(':');
            var condition = expression.substring(0, questionMarkIndex);
            var trueExpression = expression.substring(questionMarkIndex + 1, colonIndex);
            var falseExpression = expression.substring(colonIndex + 1);
            return new TernaryExpression(
                    createExpression(createParams.withExpressionAndNullTag(condition)),
                    createExpression(createParams.withExpressionAndNullTag(trueExpression)),
                    createExpression(createParams.withExpressionAndNullTag(falseExpression))
            );
        };
    }

    private static Function<CreateExpressionParams, Expression> createParenthesizedExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var innerExpression = expression.substring(1, expression.length() - 1);
            return new ParenthesizedExpression(
                    createExpression(createParams.withExpressionAndNullTag(innerExpression))
            );
        };
    }

    private static Function<CreateExpressionParams, Expression> createCastExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var typeStr = expression.substring(expression.indexOf('(') + 1, expression.indexOf(')'));
            var innerExpression = expression.substring(expression.indexOf(')') + 1);
            return new CastExpression(
                    createExpression(createParams.withExpressionAndNullTag(innerExpression)),
                    typeStr);
        };
    }

    private static Function<CreateExpressionParams, Expression> createUnaryExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var operator = ParsingUtils.extractUnaryOperator(expression);
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
        };
    }

    private static Function<CreateExpressionParams, Expression> createBinaryExpression() {
        return createParams -> {
            var expression = createParams.expression();
            var operator = ParsingUtils.extractBinaryOperator(expression);
            var firstOperandEnd = ParsingUtils.getClosingParenthesesIndex(expression);
            if (firstOperandEnd == -1) {
                firstOperandEnd = expression.indexOf(ParsingUtils.getStringOfOperator(operator));
            }
            var firstOperand = expression.substring(0, firstOperandEnd+1);
            var secondOperand = expression.substring(expression.indexOf(ParsingUtils.getStringOfOperator(operator), firstOperandEnd+1)
                    + ParsingUtils.getStringOfOperator(operator).length());
            return new BinaryExpression(
                    createExpression(createParams.withExpressionAndNullTag(firstOperand)),
                    createExpression(createParams.withExpressionAndNullTag(secondOperand)),
                    operator);
        };
    }

    public static Expression createExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        messager.printMessage(Diagnostic.Kind.NOTE, "Creating expression from " + expression);
        if (expression == null) {
            return LiteralExpression.NULL;
        }
        expression = expression.trim();
        return Type.of(expression).expressionCreator.apply(createParams);
    }
}
