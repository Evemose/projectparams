package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
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

    public static final String NULL = "superSecretDefaultValuePlaceholder";
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

    public boolean matches(MethodInvocationTree methodTree, Trees trees, TreePath path) {
        var split = methodTree.getMethodSelect().toString().split("\\.");
        var methodName = split[split.length - 1];
        String ownerQualifiedName = null;
        if (methodTree.getMethodSelect() instanceof MemberSelectTree memberSelectTree) {
            var expression = memberSelectTree.getExpression();
            var ownerTree = trees.getTree(trees.getElement(new TreePath(path, expression)));
            if (ownerTree != null) {
                if (ownerTree instanceof JCTree.JCVariableDecl varDecl) {
                    var ownerType = varDecl.type;
                    if (ownerType != null) {
                        ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                    }
                } else if (ownerTree instanceof JCTree.JCMethodDecl methodDecl) {
                    var ownerType = methodDecl.getReturnType();
                    if (ownerType != null) {
                        ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.type.toString());
                    }
                }
            }
        }
        // TODO: add support for IdentifierTree
        return doesExistingArgsMatch(methodTree.getArguments())
                    && methodName.equals(name)
                    && (ownerQualifiedName == null || ownerQualifiedName.equals(this.ownerQualifiedName));
                    // for now not considering return type
                    //&& returnTypeQualifiedName.equals(TypeUtils.getReturnType(methodTree, path).toString());
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

    private boolean doesExistingArgsMatch(ExecutableElement method) {
        var currentArgs = method.getParameters();
        return doesExistingArgsMatch(currentArgs.stream().map(arg -> TypeUtils.getBoxedTypeName(arg.asType().toString()))
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

    // required to detect overloaded methods
    public MethodInfo withNullOwnerAndDefault() {
        return new MethodInfo(name,
                null,
                returnTypeQualifiedName,
                parameterTypeQualifiedNames,
                null);
    }
}
