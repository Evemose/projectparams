package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.utils.ElementUtils;
import org.projectparams.annotations.DefaultValue;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public record InvocableInfo(
        ExecutableElement method,
        String name,
        Set<String> possibleOwnerQualifiedNames,
        String returnTypeQualifiedName,
        List<Parameter> parameters) {

    public static final String NULL = "superSecretDefaultValuePlaceholder";

    public static List<InvocableInfo> from(ExecutableElement method) {
        var mainInvocable = new InvocableInfo(
                method,
                method.getSimpleName().toString(),
                method.getSimpleName().toString().equals("<init>") ?
                        Set.of(((TypeElement) method.getEnclosingElement()).getQualifiedName().toString())
                        : getPossibleOwnerQualifiedNames(method),
                getReturnTypeQualifiedName(method),
                method.getParameters().stream().map(InvocableInfo::toParameter).toList());
        var result = new ArrayList<>(List.of(mainInvocable));
        if (method.getSimpleName().toString().equals("<init>")) {
            addThisAndSuperInvocable(result, method, mainInvocable);
        }
        return result;
    }

    private static Parameter toParameter(VariableElement parameter) {
        var defaultValueAnn = parameter.getAnnotation(DefaultValue.class);
        Expression defaultValue;
        if (defaultValueAnn == null) {
            defaultValue = null;
        } else {
            defaultValue = defaultValueAnn.value().equals(NULL) ? Expression.NULL
                    : new Expression((Type) parameter.asType(), defaultValueAnn.value());
        }
        return new Parameter(parameter.getSimpleName().toString(),
                (Type) parameter.asType(),
                defaultValue);
    }

    private static void addThisAndSuperInvocable(ArrayList<InvocableInfo> result, ExecutableElement method, InvocableInfo mainInvocable) {
        var directChildren = ElementUtils.getAllChildren((TypeElement) method.getEnclosingElement()).stream()
                .filter(child -> child.getSuperclass() != null
                        && child.getSuperclass().toString().equals(method.getEnclosingElement().asType().toString()))
                .collect(Collectors.toSet());
        for (var child : directChildren) {
            result.add(mainInvocable.withName("super")
                    .withPossibleOwnerQualifiedNames(Set.of(child.getQualifiedName().toString())));
        }
        result.add(mainInvocable.withName("this"));
    }

    private static Set<String> getPossibleOwnerQualifiedNames(ExecutableElement method) {
        var classElement = (TypeElement) method.getEnclosingElement();
        var result = new HashSet<>(Set.of(classElement.getQualifiedName().toString()));

        var allChildren = new ArrayList<>(ElementUtils.getAllChildren(classElement));

        var overridingSubclasses = allChildren.stream()
                .filter(child -> child.getEnclosedElements().stream()
                        .filter(e -> e.getKind() == ElementKind.METHOD)
                        .anyMatch(e -> isOverride((ExecutableElement) e, method)))
                .collect(Collectors.toSet());

        allChildren.removeAll(overridingSubclasses.stream().flatMap(child ->
                Stream.concat(ElementUtils.getAllChildren(child).stream(), Stream.of(child))).toList());

        result.addAll(allChildren.stream().map(el -> el.getQualifiedName().toString()).toList());

        return result;
    }

    private static boolean isOverride(ExecutableElement method, ExecutableElement superMethod) {
        return method.getSimpleName().equals(superMethod.getSimpleName())
                && method.getParameters().stream().map(VariableElement::asType).toList().equals(
                superMethod.getParameters().stream().map(VariableElement::asType).toList())
                && method.getReturnType().equals(superMethod.getReturnType());
    }

    private static String getReturnTypeQualifiedName(ExecutableElement method) {
        var name = method.getSimpleName().toString();
        if (name.equals("<init>")) {
            return ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString();
        } else {
            return method.getReturnType().toString();
        }
    }

    public InvocableInfo withName(String name) {
        return new InvocableInfo(
                method,
                name,
                possibleOwnerQualifiedNames,
                returnTypeQualifiedName,
                parameters);
    }

    public InvocableInfo withPossibleOwnerQualifiedNames(Set<String> possibleOwnerQualifiedNames) {
        return new InvocableInfo(
                method,
                name,
                possibleOwnerQualifiedNames,
                returnTypeQualifiedName,
                parameters);
    }

    public boolean matches(InvocableTree invocation) {
        var methodName = invocation.getSelfName();
        var ownerQualifiedName = invocation.getOwnerTypeQualifiedName();
        return possibleOwnerQualifiedNames.contains(ownerQualifiedName)
                && methodName.equals(name)
                && doesExistingArgsMatch(invocation.getArguments());
        // for now not considering return type
    }

    private boolean doesExistingArgsMatch(List<? extends ExpressionTree> args) {
        var currentArgs = args.stream().map(arg -> {
            if (((JCTree.JCExpression) arg).type == null) {
                return Type.noType;
            } else {
                return TypeUtils.getActualType(arg);
            }
        }).toArray(Type[]::new);
        return doesExistingArgsMatch(currentArgs);
    }

    private boolean doesExistingArgsMatch(Type... argTypes) {
        return IntStream.range(0, argTypes.length).allMatch(i ->
                TypeUtils.isAssignable(
                        TypeUtils.getBoxedType(argTypes[i]),
                        TypeUtils.getBoxedType(parameters.get(i).type)));
    }

    public String toString() {
        return String.join("|", possibleOwnerQualifiedNames) + "." + name + "(" + parameters.toString()
                .replaceAll("[\\[\\]]", "") + "): " + returnTypeQualifiedName;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InvocableInfo invocable) {
            return parameters.equals(invocable.parameters)
                    && name.equals(invocable.name) && returnTypeQualifiedName.equals(invocable.returnTypeQualifiedName)
                    && (Objects.equals(possibleOwnerQualifiedNames, invocable.possibleOwnerQualifiedNames));
        } else {
            return false;
        }
    }

    public record Expression(
            Type type,
            String expression
    ) {
        public static final Expression NULL = new Expression(Type.noType, null);
    }

    public record Parameter(
            String name,
            Type type,
            Expression defaultValue
    ) {
        @Override
        public String toString() {
            return name + (defaultValue != null ? "=" + defaultValue.expression() : "") + " : " + type;
        }
    }
}
