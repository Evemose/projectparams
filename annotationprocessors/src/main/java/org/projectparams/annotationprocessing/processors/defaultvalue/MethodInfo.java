package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import org.projectparams.annotationprocessing.ast.TypeUtils;
import org.projectparams.annotations.DefaultValue;

import javax.lang.model.element.ExecutableElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record MethodInfo(String name,
                         String ownerQualifiedName,
                         String returnTypeQualifiedName,
                         String[] parameterTypeQualifiedNames,
                         Map<Integer, Object> paramIndexToDefaultValue) {

    public static final Object NULL = new Object();
    public static MethodInfo from(ExecutableElement method) {
        return new MethodInfo(method.getSimpleName().toString(),
                method.getEnclosingElement().toString(),
                method.getReturnType().toString(),
                method.getParameters().stream().map(parameter ->
                        parameter.asType().toString()).toArray(String[]::new),
                getDefaultValuesMap(method));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<Integer, Object> getDefaultValuesMap(ExecutableElement method) {
        return IntStream.range(0, method.getParameters().size())
                .filter(index -> method.getParameters().get(index).getAnnotation(DefaultValue.class) != null)
                .boxed()
                .collect(Collectors.toMap(index -> index,
                        index -> {
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
                                    case "java.lang.Boolean", "boolean" -> Boolean.valueOf(value);
                                    default -> throw new RuntimeException("Unsupported type: " + paramType);
                                };
                            }
                        })
                );
    }

    static int cout = 0;
    public boolean matches(MethodInvocationTree methodTree, Trees trees, TreePath path) {
        var methodCall = trees.getElement(path);
        if (methodCall instanceof ExecutableElement methodCallExecutable) {
            return doesExistingArgsMatch(methodCallExecutable)
                    && doesReturnTypeMatch(methodCallExecutable)
                    && methodCall.getSimpleName().toString().equals(name)
                    && methodCall.getEnclosingElement().toString().equals(ownerQualifiedName);
        } else {
            var split = methodTree.getMethodSelect().toString().split("\\.");
            var methodName = split[split.length - 1];
            if (methodName.equals("bibus")) {
                cout++;
            }
            return doesExistingArgsMatch(methodTree.getArguments(), path)
                    && methodName.equals(name);
                    // for now not considering return type
                    //&& returnTypeQualifiedName.equals(TypeUtils.getReturnType(methodTree, path).toString());
        }
    }

    // TODO: fix tomorrow
    private boolean doesExistingArgsMatch(List<? extends ExpressionTree> args, TreePath path) {
        var currentArgs = args.stream().map(arg -> {
            if (arg instanceof MethodInvocationTree methodInvocationTree) {
                return TypeUtils.getReturnType(methodInvocationTree, path).toString();
            } else if (arg instanceof LiteralTree literalTree) {
                return literalTree.getValue().getClass().toString().replaceAll("class ", "");
            } else if (arg instanceof IdentifierTree identifierTree) {
                return "placeholder";
            } else {
                return arg.toString();
            }
        }).toArray(String[]::new);
        return doesExistingArgsMatch(currentArgs);
    }

    private boolean doesExistingArgsMatch(ExecutableElement method) {
        var currentArgs = method.getParameters();
        return doesExistingArgsMatch(currentArgs.stream().map(arg -> arg.asType().toString())
                .toArray(String[]::new));
    }

    private boolean doesExistingArgsMatch(String[] argTypeNames) {
        return Arrays.equals(
                Arrays.stream(Arrays.copyOf(parameterTypeQualifiedNames, argTypeNames.length))
                        .map(TypeUtils::getBoxedTypeName).toArray(String[]::new),
                argTypeNames);
    }

    private boolean doesReturnTypeMatch(ExecutableElement method) {
        return method.getReturnType().toString().equals(returnTypeQualifiedName);
    }
}
