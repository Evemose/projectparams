package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.Nullable;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: implement type casting
@SuppressWarnings("unused")
public class ExpressionFactory {
    public static Messager messager;
    public enum Type {
        NEW_CLASS,
        METHOD_INVOCATION,
        LITERAL,
        IDENTIFIER,
        TERNARY,
        PARENTHESIZED,
        BINARY,
        UNARY,
        CAST,
        FIELD_ACCESS;

        public static Type of(String expression) {
            expression = expression.strip();
            if (expression.matches("\\(.+\\)") && !expression.matches("\\((\\w+\\s*(\\.)?)+\\).+")) {
                return PARENTHESIZED;
            }
            if (expression.matches("[^(]+\\?.+:.+")) {
                return TERNARY;
            }
            if (expression.matches(".+(\\+|-|\\*|/|%|&|\\||\\^|<<|>>|>>>|<|>|<=|>=|==|!=|&&|\\|\\||instanceof).+")
            && !expression.matches(".+<(\\w|\\s|,)*>((\\w|\\s)+)?(\\(.*\\))?")) {
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
                var argsStartIndex = getArgsStartIndex(expression);
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
            if (expression.contains(".")) {
                return FIELD_ACCESS;
            }
            return IDENTIFIER;
        }
    }

    private static int getArgsStartIndex(String expression) {
        var i = expression.length() - 1;
        var parenthesesCount = 0;
        while (i >= 0) {
            var c = expression.charAt(i);
            if (c == ')') {
                parenthesesCount++;
            } else if (c == '(') {
                if (parenthesesCount == 1) {
                    return i;
                }
                parenthesesCount--;
            }
            i--;
        }
        return -1;
    }

    @SuppressWarnings("all")
    public static Expression createExpression(String expression,
                                              @Nullable TypeTag typeTag,
                                              TreePath enclosingInvocablePath) {
        messager.printMessage(Diagnostic.Kind.NOTE, "Creating expression from " + expression);
        if (expression == null) {
            return LiteralExpression.NULL;
        }
        expression = expression.trim();
        var type = Type.of(expression);
        messager.printMessage(Diagnostic.Kind.NOTE, "Expression type is " + type);
        return switch (type) {
            case LITERAL -> {
                // infer type tag of literal if it is not provided explicitly
                if (typeTag == null) {
                    typeTag = TypeUtils.geLiteralTypeTag(expression);
                }
                var value = TypeUtils.literalValueFromStr(typeTag, expression);
                yield new LiteralExpression(value, value.getClass());
            }
            case METHOD_INVOCATION -> {
                var argsStartIndex = getArgsStartIndex(expression);
                var args = getArgStrings(expression);
                var ownerSeparatorIndex = expression.lastIndexOf('.', argsStartIndex);
                var typeArgsEndIndex = expression.lastIndexOf('>', argsStartIndex);
                if (typeArgsEndIndex == -1) {
                    typeArgsEndIndex = ownerSeparatorIndex;
                }
                var name = expression.substring(typeArgsEndIndex + 1, argsStartIndex);
                Expression owner = null;
                if (ownerSeparatorIndex != -1) {
                    var ownerExpression = expression.substring(0, ownerSeparatorIndex);
                    owner = createExpression(ownerExpression, null, enclosingInvocablePath);
                }
                List<String> typeParameters = new ArrayList<>();
                if (typeArgsEndIndex != ownerSeparatorIndex) {
                    var typeArgs = expression.substring(expression.lastIndexOf('<', typeArgsEndIndex) + 1, typeArgsEndIndex);
                    var typeArgsParts = typeArgs.split(",");
                    for (var typeArg : typeArgsParts) {
                        typeParameters.add(typeArg.strip());
                    }
                }
                yield new MethodInvocationExpression(name.strip(),
                        owner,
                        args.stream().map(arg -> createExpression(arg, null, enclosingInvocablePath)).toList(),
                        enclosingInvocablePath,
                        typeParameters.stream().map(typearg -> createExpression(typearg,null, enclosingInvocablePath)).toList());
            }
            case FIELD_ACCESS -> {
                var lastDotIndex = expression.lastIndexOf('.');
                var owner = expression.substring(0, lastDotIndex);
                var name = expression.substring(lastDotIndex + 1);
                yield new FieldAccessExpression(name, createExpression(owner, null, enclosingInvocablePath));
            }
            case NEW_CLASS -> {
                var argsStartIndex = getArgsStartIndex(expression);
                var args = getArgStrings(expression);
                var newKeywordIndex = expression.lastIndexOf("new ", argsStartIndex);
                var typeArgsStartIndex = expression.indexOf('<', newKeywordIndex);
                if (typeArgsStartIndex == -1) {
                    typeArgsStartIndex = argsStartIndex;
                }
                var name = expression.substring(newKeywordIndex + 4, typeArgsStartIndex);
                var ownerSeparatorIndex = expression.lastIndexOf('.', newKeywordIndex);
                Expression owner = null;
                if (ownerSeparatorIndex != -1) {
                    var ownerExpression = expression.substring(0, ownerSeparatorIndex);
                    owner = createExpression(ownerExpression, null, enclosingInvocablePath);
                }
                List<String> typeParameters = new ArrayList<>();
                if (typeArgsStartIndex != argsStartIndex) {
                    var typeArgs = expression.substring(typeArgsStartIndex + 1, expression.lastIndexOf('>', argsStartIndex));
                    var typeArgsParts = typeArgs.split(",");
                    for (var typeArg : typeArgsParts) {
                        typeParameters.add(typeArg.strip());
                    }
                }
                yield new NewClassExpression(name,
                        owner,
                        args.stream().map(arg -> createExpression(arg, null, enclosingInvocablePath)).toList(),
                        enclosingInvocablePath,
                        typeParameters.stream().map(typearg -> createExpression(typearg,null, enclosingInvocablePath)).toList());
            }
            case IDENTIFIER -> {
                yield new IdentifierExpression(expression);
            }
            case TERNARY -> {
                var questionMarkIndex = expression.indexOf('?');
                var colonIndex = expression.indexOf(':');
                var condition = expression.substring(0, questionMarkIndex);
                var trueExpression = expression.substring(questionMarkIndex + 1, colonIndex);
                var falseExpression = expression.substring(colonIndex + 1);
                yield new TernaryExpression(createExpression(condition, null, enclosingInvocablePath),
                        createExpression(trueExpression, null, enclosingInvocablePath),
                        createExpression(falseExpression, null, enclosingInvocablePath));
            }
            case PARENTHESIZED -> {
                var innerExpression = expression.substring(1, expression.length() - 1);
                yield new ParenthesizedExpression(createExpression(innerExpression, null, enclosingInvocablePath));
            }
            case CAST -> {
                var typeStr = expression.substring(expression.indexOf('(') + 1, expression.indexOf(')'));
                var innerExpression = expression.substring(expression.indexOf(')') + 1);
                yield new CastExpression(createExpression(innerExpression, null, enclosingInvocablePath), typeStr);
            }
            case UNARY -> {
                var operator = extractUnaryOperator(expression);
                String operand;
                if (operator == JCTree.Tag.POSTDEC || operator == JCTree.Tag.POSTINC) {
                    operand = expression.substring(0, expression.length() - 2);
                } else {
                    operand = expression.substring(expression.indexOf(getStringOfOperator(operator)) + getStringOfOperator(operator).length());
                }
                yield new UnaryExpression(createExpression(operand, null, enclosingInvocablePath), operator);
            }
            case BINARY -> {
                var operator = extractBinaryOperator(expression);
                var firstOperandEnd = getClosingParenthesesIndex(expression, 0);
                if (firstOperandEnd == -1) {
                    firstOperandEnd = expression.indexOf(getStringOfOperator(operator));
                }
                var firstOperand = expression.substring(0, firstOperandEnd+1);
                var secondOperand = expression.substring(expression.indexOf(getStringOfOperator(operator), firstOperandEnd+1)
                        + getStringOfOperator(operator).length());
                yield new BinaryExpression(createExpression(firstOperand, null, enclosingInvocablePath),
                        createExpression(secondOperand, null, enclosingInvocablePath),
                        operator);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private static String getStringOfOperator(JCTree.Tag operatorTag) {
        return switch (operatorTag) {
            case PLUS, POS -> "+";
            case MINUS, NEG -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case BITAND -> "&";
            case BITOR -> "|";
            case BITXOR -> "^";
            case SL -> "<<";
            case SR -> ">>";
            case USR -> ">>>";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case EQ -> "==";
            case NE -> "!=";
            case AND -> "&&";
            case OR -> "||";
            case TYPETEST -> "instanceof";
            case NOT -> "!";
            case COMPL -> "~";
            case PREINC -> "++";
            case PREDEC -> "--";
            case POSTINC -> "++";
            case POSTDEC -> "--";
            default -> throw new IllegalArgumentException("Unknown operator tag " + operatorTag);
        };
    }

    private static JCTree.Tag extractBinaryOperator(String expression) {
        expression = expression.strip();
        var firstOperandEnd = getClosingParenthesesIndex(expression, 0);
        var substring = expression.substring(firstOperandEnd+1).trim();
        if (substring.startsWith("+")) {
            return JCTree.Tag.PLUS;
        }
        if (substring.startsWith("-")) {
            return JCTree.Tag.MINUS;
        }
        if (substring.startsWith("*")) {
            return JCTree.Tag.MUL;
        }
        if (substring.startsWith("/")) {
            return JCTree.Tag.DIV;
        }
        if (substring.startsWith("%")) {
            return JCTree.Tag.MOD;
        }
        if (substring.startsWith("&&")) {
            return JCTree.Tag.AND;
        }
        if (substring.startsWith("||")) {
            return JCTree.Tag.OR;
        }
        if (substring.startsWith("&")) {
            return JCTree.Tag.BITAND;
        }
        if (substring.startsWith("|")) {
            return JCTree.Tag.BITOR;
        }
        if (substring.startsWith("^")) {
            return JCTree.Tag.BITXOR;
        }
        if (substring.startsWith("<<")) {
            return JCTree.Tag.SL;
        }
        if (substring.startsWith(">>")) {
            return JCTree.Tag.SR;
        }
        if (substring.startsWith(">>>")) {
            return JCTree.Tag.USR;
        }
        if (substring.startsWith("<")) {
            return JCTree.Tag.LT;
        }
        if (substring.startsWith(">")) {
            return JCTree.Tag.GT;
        }
        if (substring.startsWith("<=")) {
            return JCTree.Tag.LE;
        }
        if (substring.startsWith(">=")) {
            return JCTree.Tag.GE;
        }
        if (substring.startsWith("==")) {
            return JCTree.Tag.EQ;
        }
        if (substring.startsWith("!=")) {
            return JCTree.Tag.NE;
        }
        if (substring.startsWith("instanceof")) {
            return JCTree.Tag.TYPETEST;
        }
        throw new IllegalArgumentException("Unknown binary operator in " + expression);
    }

    @SuppressWarnings("all")
    private static int getClosingParenthesesIndex(String expression, int openingParenthesesIndex) {
        var parenthesesCount = 0;
        var i = openingParenthesesIndex;
        while (i < expression.length()) {
            var c = expression.charAt(i);
            if (c == '(') {
                parenthesesCount++;
            } else if (c == ')') {
                parenthesesCount--;
                if (parenthesesCount == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    private static JCTree.Tag extractUnaryOperator(String expression) {
        expression = expression.strip();
        if (expression.startsWith("++")) {
            return JCTree.Tag.PREINC;
        }
        if (expression.startsWith("--")) {
            return JCTree.Tag.PREDEC;
        }
        if (expression.startsWith("+")) {
            return JCTree.Tag.POS;
        }
        if (expression.startsWith("-")) {
            return JCTree.Tag.NEG;
        }
        if (expression.startsWith("!")) {
            return JCTree.Tag.NOT;
        }
        if (expression.startsWith("~")) {
            return JCTree.Tag.COMPL;
        }
        if (expression.endsWith("++")) {
            return JCTree.Tag.POSTINC;
        }
        if (expression.endsWith("--")) {
            return JCTree.Tag.POSTDEC;
        }
        throw new IllegalArgumentException("Unknown unary operator in " + expression);
    }

    private static List<String> getArgStrings(String expression) {
        try {
            if (expression.endsWith("()")) {
                return Collections.emptyList();
            }
            var argsString = expression.substring(getArgsStartIndex(expression) + 1, expression.length() - 1);
            var parenthesesCount = 0;
            var args = new ArrayList<String>();
            var argBeginIndex = 0;
            for (var i = 0; i < argsString.length(); i++) {
                var c = argsString.charAt(i);
                if (c == '(') {
                    parenthesesCount++;
                } else if (c == ')') {
                    parenthesesCount--;
                } else if (c == ',' && parenthesesCount == 0) {
                    if (argBeginIndex == i || argsString.substring(argBeginIndex, i).matches("\\s*")) {
                        throw new IllegalArgumentException("Empty argument in " + expression);
                    }
                    args.add(argsString.substring(argBeginIndex, i));
                    argBeginIndex = i + 1;
                }
            }
            if (parenthesesCount != 0) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Unbalanced parentheses in " + argsString + " count: " + parenthesesCount);
                throw new IllegalArgumentException("Unbalanced parentheses in " + expression);
            }
            if (argBeginIndex == argsString.length()) {
                throw new IllegalArgumentException("Empty argument in " + expression);
            }
            args.add(argsString.substring(argBeginIndex));
            return args;
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unbalanced parentheses in " + expression);
        }
    }
}
