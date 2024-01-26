package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
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

public record InvocableInfo(String name,
                            Set<String> possibleOwnerQualifiedNames,
                            String returnTypeQualifiedName,
                            List<String> parameterTypeQualifiedNames,
                            Map<Integer, Object> paramIndexToDefaultValue) {

    public static final String NULL = "superSecretDefaultValuePlaceholder";

    public static InvocableInfo from(ExecutableElement method) {
        return new InvocableInfo(method.getSimpleName().toString(),
                method.getSimpleName().toString().equals("<init>") ?
                        Set.of(((TypeElement) method.getEnclosingElement()).getQualifiedName().toString())
                        : getPossibleOwnerQualifiedNames(method),
                getReturnTypeQualifiedName(method),
                method.getParameters().stream().map(parameter ->
                        parameter.asType().toString()).toList(),
                getDefaultValuesMap(method));
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
        return method.getSimpleName().toString().equals(superMethod.getSimpleName().toString())
                && method.getParameters().stream().map(VariableElement::asType).equals(
                superMethod.getParameters().stream().map(VariableElement::asType))
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
                                    case "java.lang.Character", "char" -> value.charAt(0);
                                    case "java.lang.Byte", "byte" -> Byte.valueOf(value);
                                    case "java.lang.Short", "short" -> Short.valueOf(value);
                                    default -> throw new IllegalArgumentException("Unsupported type: " + paramType);
                                };
                            }
                        })
                );
    }

    public boolean matches(InvocableTree invocation) {
        var methodName = invocation.getSelfName();
        var ownerQualifiedName = invocation.getOwnerTypeQualifiedName();
        return possibleOwnerQualifiedNames.contains(ownerQualifiedName)
                && methodName.equals(name)
                && doesExistingArgsMatch(invocation.getArguments());
        // for now not considering return type
    }

    // TODO: fix
    private boolean doesExistingArgsMatch(List<? extends ExpressionTree> args) {
        var currentArgs = args.stream().map(arg -> {
            if (((JCTree.JCExpression) arg).type == null) {
                return "superSecretErrTypePlaceholder";
            } else {
                return TypeUtils.getBoxedTypeName(TypeUtils.getActualType(arg).toString());
            }
        }).toArray(String[]::new);
        return doesExistingArgsMatch(currentArgs);
    }

    private boolean doesExistingArgsMatch(String[] argTypeNames) {
        return IntStream.range(0, argTypeNames.length).allMatch(i ->
                TypeUtils.isAssignable(
                        TypeUtils.getTypeByName(TypeUtils.getBoxedTypeName(argTypeNames[i])),
                        TypeUtils.getTypeByName(TypeUtils.getBoxedTypeName(parameterTypeQualifiedNames.get(i)))));
    }

    public String toString() {
        return String.join("|", possibleOwnerQualifiedNames) + "." + name + "(" + parameterTypeQualifiedNames.toString()
                .replaceAll("[\\[\\]]", "") + "): " + returnTypeQualifiedName;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InvocableInfo invocable) {
            return Objects.equals(parameterTypeQualifiedNames, invocable.parameterTypeQualifiedNames)
                    && name.equals(invocable.name) && returnTypeQualifiedName.equals(invocable.returnTypeQualifiedName)
                    && (Objects.equals(possibleOwnerQualifiedNames, invocable.possibleOwnerQualifiedNames));
        } else {
            return false;
        }
    }
}
