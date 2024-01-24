package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotations.DefaultValue;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record InvocableInfo(String name,
                            String ownerQualifiedName,
                            String returnTypeQualifiedName,
                            String[] parameterTypeQualifiedNames,
                            Map<Integer, Object> paramIndexToDefaultValue) {

    public static final String NULL = "superSecretDefaultValuePlaceholder";

    public static InvocableInfo from(ExecutableElement method) {
        return new InvocableInfo(method.getSimpleName().toString(),
                method.getEnclosingElement().toString(),
                getReturnTypeQualifiedName(method),
                method.getParameters().stream().map(parameter ->
                        parameter.asType().toString()).toArray(String[]::new),
                getDefaultValuesMap(method));
    }

    private static String getReturnTypeQualifiedName(ExecutableElement method) {
        var name = method.getSimpleName().toString();
        if (name.equals("<init>")) {
            return ((TypeElement)method.getEnclosingElement()).getQualifiedName().toString();
        } else {
            return method.getReturnType().toString();
        }
    }

    private static Map<Integer, Object> getDefaultValuesMap(ExecutableElement method) {
        return IntStream.range(0, method.getParameters().size())
                .filter(index -> method.getParameters().get(index).getAnnotation(DefaultValue.class) != null)
                .boxed()
                .collect(Collectors.toMap(index -> index,
                        index -> {
                            // TODO: test enums
                            var value = method.getParameters().get(index).getAnnotation(DefaultValue.class).value();
                            if (value.equals("superSecretDefaultValuePlaceholder")) {
                                return NULL;
                            } else {
                                var paramType = method.getParameters().get(index).asType().toString();
                                return switch (paramType) {
                                    case "java.lang.String" -> value;
                                    case "java.lang.Integer", "int" -> Integer.valueOf(value);
                                    case "java.lang.Long", "long" -> Long.valueOf(value);
                                    case "java.lang.Float", "float" -> Float.valueOf(value);
                                    case "java.lang.Double", "double" -> Double.valueOf(value);
                                    case "java.lang.Boolean", "boolean" -> value.equals("true") ? 1 : 0;
                                    default -> throw new RuntimeException("Unsupported type: " + paramType);
                                };
                            }
                        })
                );
    }

    public boolean matches(InvocableTree invocation) {
        var methodName = invocation.getSelfName();
        var ownerQualifiedName = invocation.getOwnerTypeQualifiedName();
        return (ownerQualifiedName == null || ownerQualifiedName.equals(this.ownerQualifiedName))
                && methodName.equals(name)
                && doesExistingArgsMatch(invocation.getArguments());
        // for now not considering return type
    }

    public boolean matchesExact(InvocableTree invocation) {
        var methodName = invocation.getSelfName();
        var ownerQualifiedName = invocation.getOwnerTypeQualifiedName();
        return (ownerQualifiedName == null || ownerQualifiedName.equals(this.ownerQualifiedName))
                && methodName.equals(name)
                && doesAllArgsMatchExact(invocation.getArguments());
        // for now not considering return type
    }

    private boolean doesAllArgsMatchExact(List<? extends ExpressionTree> args) {
        var currentArgs = args.stream().map(arg -> {
            if (((JCTree.JCExpression) arg).type == null) {
                return "superSecretErrTypePlaceholder";
            } else {
                return TypeUtils.getBoxedTypeName(((JCTree.JCExpression) arg).type.toString());
            }
        }).toArray(String[]::new);
        return Arrays.equals(parameterTypeQualifiedNames, currentArgs);
    }

    // TODO: fix
    private boolean doesExistingArgsMatch(List<? extends ExpressionTree> args) {
        var currentArgs = args.stream().map(arg -> {
            if (((JCTree.JCExpression) arg).type == null) {
                return "superSecretErrTypePlaceholder";
            } else {
                return TypeUtils.getBoxedTypeName(((JCTree.JCExpression) arg).type.toString());
            }
        }).toArray(String[]::new);
        return doesExistingArgsMatch(currentArgs);
    }

    private boolean doesExistingArgsMatch(String[] argTypeNames) {
        return Arrays.equals(
                Arrays.stream(Arrays.copyOf(parameterTypeQualifiedNames, argTypeNames.length))
                        .map(TypeUtils::getBoxedTypeName).toArray(String[]::new),
                argTypeNames);
    }

    public String toString() {
        return ownerQualifiedName + "." + name + "(" + Arrays.toString(parameterTypeQualifiedNames)
                .replaceAll("[\\[\\]]", "") + "): " + returnTypeQualifiedName;
    }
}
