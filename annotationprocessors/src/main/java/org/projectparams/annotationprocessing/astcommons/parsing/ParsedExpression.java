package org.projectparams.annotationprocessing.astcommons.parsing;

import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: implement type casting
@SuppressWarnings("unused")
public record ParsedExpression(
        ParsedExpression.Type type,
        // value for literals, name for field access and method invocation
        String name,
        ParsedExpression owner,
        List<ParsedExpression> arguments,
        @Nullable com.sun.tools.javac.code.Type returnType
) {
    public enum Type {
        NEW_CLASS,
        METHOD_INVOCATION,
        LITERAL,
        IDENTIFIER_OR_FIELD_ACCESS,
        FIELD_ACCESS;

        public static Type of(String expression) {
            expression = expression.strip();
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
            return IDENTIFIER_OR_FIELD_ACCESS;
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

    public static ParsedExpression from(InvocableInfo.Expression expression) {
        var stringOfExpr = expression.expression();
        if (stringOfExpr == null) {
            return new ParsedExpression(Type.LITERAL, null, null, Collections.emptyList(), expression.type());
        }
        stringOfExpr = stringOfExpr.strip();
        var type = Type.of(stringOfExpr);
        if (type == Type.LITERAL) {
            return new ParsedExpression(type, stringOfExpr, null, Collections.emptyList(), expression.type());
        }

        if (type == Type.FIELD_ACCESS) {
            var lastDotIndex = stringOfExpr.lastIndexOf('.');
            if (lastDotIndex == -1) {
                return new ParsedExpression(type, stringOfExpr, null, Collections.emptyList(), expression.type());
            }
        }

        var args = Collections.<ParsedExpression>emptyList();
        if (type == Type.NEW_CLASS || type == Type.METHOD_INVOCATION) {
            args = getArgStrings(stringOfExpr).stream()
                    .map(str -> from(new InvocableInfo.Expression(null, str)))
                    .toList();
        }
        ParsedExpression owner = null;
        String name;

        if (type == Type.NEW_CLASS) {
            name = stringOfExpr.substring(stringOfExpr.lastIndexOf("new ") + 4);
            name = name.substring(0, name.indexOf('('));
        } else if (type == Type.METHOD_INVOCATION) {
            var argsStartIndex = getArgsStartIndex(stringOfExpr);
            name = stringOfExpr.substring(stringOfExpr.lastIndexOf('.', argsStartIndex) + 1, argsStartIndex);
        } else {
            name = stringOfExpr.substring(stringOfExpr.lastIndexOf('.') + 1);
        }

        int lastDotIndex;
        if (type == Type.FIELD_ACCESS) {
            lastDotIndex = stringOfExpr.lastIndexOf('.');
        } else if (type == Type.IDENTIFIER_OR_FIELD_ACCESS) {
            lastDotIndex = -1;
        } else {
            var argsStartIndex = getArgsStartIndex(stringOfExpr);
            lastDotIndex = stringOfExpr.lastIndexOf('.', argsStartIndex);
        }
        if (lastDotIndex != -1) {
            owner = from(new InvocableInfo.Expression(null, stringOfExpr.substring(0, lastDotIndex)));
        }

        return new ParsedExpression(type, name, owner, args, expression.type());
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
