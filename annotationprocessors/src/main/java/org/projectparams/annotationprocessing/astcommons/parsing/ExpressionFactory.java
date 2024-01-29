package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.TypeTag;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: implement type casting
@SuppressWarnings("unused")
public class ExpressionFactory {
    public enum Type {
        NEW_CLASS,
        METHOD_INVOCATION,
        LITERAL,
        IDENTIFIER,
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
        if (expression == null) {
            return LiteralExpression.NULL;
        }
        expression = expression.strip();
        var type = Type.of(expression);
        switch (type) {
            case LITERAL -> {
                // infer type tag of literal if it is not provided explicitly
                if (typeTag == null) {
                    typeTag = TypeUtils.geLiteralTypeTag(expression);
                }
                var value = TypeUtils.literalValueFromStr(typeTag, expression);
                return new LiteralExpression(value, value.getClass());
            }
            case METHOD_INVOCATION -> {
                var argsStartIndex = getArgsStartIndex(expression);
                var args = getArgStrings(expression);
                var name = expression.substring(expression.lastIndexOf('.') + 1, argsStartIndex);
                var lastDotIndex = expression.lastIndexOf('.', argsStartIndex - 1);
                Expression owner = null;
                if (lastDotIndex != -1) {
                    var ownerExpression = expression.substring(0, lastDotIndex);
                    owner = createExpression(ownerExpression, null, enclosingInvocablePath);
                }
                return new MethodInvocationExpression(name,
                        owner,
                        args.stream().map(arg -> createExpression(arg, null, enclosingInvocablePath)).toList(),
                        enclosingInvocablePath);
            }
            case FIELD_ACCESS -> {
                var lastDotIndex = expression.lastIndexOf('.');
                var owner = expression.substring(0, lastDotIndex);
                var name = expression.substring(lastDotIndex + 1);
                return new FieldAccessExpression(name, createExpression(owner, null, enclosingInvocablePath));
            }
            case NEW_CLASS -> {
                var argsStartIndex = getArgsStartIndex(expression);
                var args = getArgStrings(expression);
                var name = expression.substring(expression.lastIndexOf("new ", argsStartIndex) + 4, argsStartIndex);
                var lastDotIndex = expression.lastIndexOf('.', argsStartIndex - 1);
                Expression owner = null;
                if (lastDotIndex != -1) {
                    var ownerExpression = expression.substring(0, lastDotIndex);
                    owner = createExpression(ownerExpression, null, enclosingInvocablePath);
                }
                return new NewClassExpression(name,
                        owner,
                        args.stream().map(arg -> createExpression(arg, null, enclosingInvocablePath)).toList(),
                        enclosingInvocablePath);
            }
            case IDENTIFIER -> {
                return new IdentifierExpression(expression);
            }
            default -> throw new IllegalStateException("Unexpected value: " + type);
        }
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
