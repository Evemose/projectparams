package org.projectparams.annotationprocessing.astcommons.parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public record ParsedExpression(
        ParsedExpression.Type type,
        String name,
        ParsedExpression owner,
        List<ParsedExpression> arguments
) {
    public enum Type {
        NEW_CLASS,
        METHOD_INVOCATION,
        LOCAL_FIELD_ACCESS,
        LITERAL,
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
            if (expression.contains(".") && !(expression.startsWith("this.") || expression.startsWith("super."))) {
                return FIELD_ACCESS;
            } else {
                expression = expression.substring(expression.lastIndexOf('.') + 1);
                if (expression.matches("(\\d+(\\.\\d+)?[fdlFDL]?)|(true|false)")) {
                    return LITERAL;
                } else {
                    return LOCAL_FIELD_ACCESS;
                }
            }
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

    public static ParsedExpression from(String expression) {
        expression = expression.strip();
        var type = Type.of(expression);
        var args = Collections.<ParsedExpression>emptyList();
        if (type == Type.NEW_CLASS || type == Type.METHOD_INVOCATION) {
            args = getArgStrings(expression).stream()
                    .map(ParsedExpression::from)
                    .toList();
        }
        ParsedExpression owner = null;
        String name;

        if (type == Type.NEW_CLASS) {
            name = expression.substring(expression.lastIndexOf("new ") + 4);
            name = name.substring(0, name.indexOf('('));
        } else if (type == Type.METHOD_INVOCATION) {
            var argsStartIndex = getArgsStartIndex(expression);
            name = expression.substring(expression.lastIndexOf('.', argsStartIndex) + 1, argsStartIndex);
        } else {
            name = expression.substring(expression.lastIndexOf('.') + 1);
        }

        if (type != Type.LITERAL && type != Type.LOCAL_FIELD_ACCESS) {
            int lastDotIndex;
            if (type == Type.FIELD_ACCESS) {
                lastDotIndex = expression.lastIndexOf('.');
            } else {
                var argsStartIndex = getArgsStartIndex(expression);
                lastDotIndex = expression.lastIndexOf('.', argsStartIndex);
            }
            if (lastDotIndex != -1) {
                owner = from(expression.substring(0, lastDotIndex));
            }
        }

        return new ParsedExpression(type, name, owner, args);
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
