package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.ast.TypeUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record MethodInfo(String name, String ownerQualifiedName, String returnTypeQualifiedName,
                         String[] parameterTypeQualifiedNames) {
    public static MethodInfo from(ExecutableElement method) {
        return new MethodInfo(method.getSimpleName().toString(), method.getEnclosingElement().toString(), method.getReturnType().toString(), method.getParameters().stream().map(parameter -> parameter.asType().toString()).toArray(String[]::new));
    }

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
                return literalTree.getValue().getClass().getName();
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
        return doesExistingArgsMatch(currentArgs.stream().map(TypeUtils::getQualifiedTypeName).toArray(String[]::new));
    }

    private boolean doesExistingArgsMatch(String[] argTypeNames) {
        return Arrays.equals(Arrays.copyOf(parameterTypeQualifiedNames, argTypeNames.length), argTypeNames);
    }

    private boolean doesReturnTypeMatch(ExecutableElement method) {
        return method.getReturnType().toString().equals(returnTypeQualifiedName);
    }
}
