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
    public static Expression from(String expression,
                                  @Nullable TypeTag typeTag,
                                  TreePath enclosingInvocablePath) {
        if (expression == null) {
            return new LiteralExpression<>(null, String.class);
        }
        // infer type tag from expression if it is not provided explicitly
        if (typeTag == null) {
            typeTag = TypeUtils.geLiteralTypeTag(expression);
        }
        expression = expression.strip();
        var type = Type.of(expression);
        switch (type) {
            case LITERAL -> {
                var value = TypeUtils.literalValueFromStr(typeTag, expression);
                return new LiteralExpression(value, value.getClass());
            }
            case METHOD_INVOCATION -> {
                var argsStartIndex = getArgsStartIndex(expression);
                var args = getArgStrings(expression);
                var name = expression.substring(expression.lastIndexOf('.') + 1, argsStartIndex);
                var owner = from(expression.substring(0, expression.lastIndexOf('.')), null, enclosingInvocablePath);
                return new MethodInvocationExpression(name,
                        owner,
                        args.stream().map(arg -> from(arg, null, enclosingInvocablePath)).toList(),
                        enclosingInvocablePath);
            }
            case FIELD_ACCESS -> {
                var lastDotIndex = expression.lastIndexOf('.');
                var owner = expression.substring(0, lastDotIndex);
                var name = expression.substring(lastDotIndex + 1);
                return new FieldAccessExpression(name, from(owner, null, enclosingInvocablePath));
            }
            case NEW_CLASS -> throw new UnsupportedOperationException();
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
