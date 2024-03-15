package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
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
        List<String> genericTypeNames,
        List<Parameter> parameters) {

    private static final String NULL = "superSecretDefaultValuePlaceholder";

    public static List<InvocableInfo> from(ExecutableElement method) {
        var mainInvocable = new InvocableInfo(
                method,
                method.getSimpleName().toString(),
                method.getSimpleName().toString().equals("<init>") ?
                        Set.of(((TypeElement) method.getEnclosingElement()).getQualifiedName().toString())
                        : getPossibleOwnerQualifiedNames(method),
                getReturnTypeQualifiedName(method),
                ElementUtils.getGenericTypeNames(method),
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
                defaultValue
        );
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
                genericTypeNames,
                parameters);
    }

    public InvocableInfo withPossibleOwnerQualifiedNames(Set<String> possibleOwnerQualifiedNames) {
        return new InvocableInfo(
                method,
                name,
                possibleOwnerQualifiedNames,
                returnTypeQualifiedName,
                genericTypeNames,
                parameters);
    }

    public boolean matches(InvocableTree invocation) {
        var methodName = invocation.getSelfName();
        var ownerQualifiedName = invocation.getOwnerTypeQualifiedName();
        if (ownerQualifiedName == null) {
            return false;
        }
        return possibleOwnerQualifiedNames.contains(ownerQualifiedName)
                && methodName.equals(name)
                && doesExistingArgsMatch(invocation.getArguments());
    }

    private boolean doesExistingArgsMatch(List<? extends ExpressionTree> args) {
        var currentArgs = args.stream().map(TypeUtils::getActualType).toArray(Type[]::new);
        return doesExistingArgsMatch(currentArgs);
    }

    private static List<Type> getGenericTypesIn(Type type) {
        var result = new ArrayList<Type>();
        switch (type) {
            case Type.ClassType classType -> {
                result.addAll(classType.getTypeArguments().map(InvocableInfo::getGenericTypesIn)
                        .stream().flatMap(Collection::stream).toList());
            }
            case Type.TypeVar typeVar -> result.add(typeVar);
            case Type.WildcardType wildcardType -> {
                result.addAll(getGenericTypesIn(wildcardType.type));
            }
            case Type.ArrayType arrayType -> result.addAll(getGenericTypesIn(arrayType.getComponentType()));
            default -> {}
        }
        return result;
    }

    private Optional<Type> extractActualType(Type source, Type genericOwner, Type genericType) {
        return switch (source) {
            case Type.ClassType classType -> {
                if (genericOwner.equals(genericType)) {
                    yield Optional.of(classType);
                }
                yield IntStream.range(0, classType.getTypeArguments().size())
                        .mapToObj(i -> extractActualType(
                                classType.getTypeArguments().get(i),
                                genericOwner.getTypeArguments().get(i),
                                genericType)
                        ).filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst();
            }
            case Type.TypeVar typeVar -> {
                if (genericOwner.equals(genericType)) {
                    yield Optional.of(typeVar);
                } else {
                    yield Optional.empty();
                }
            }
            case Type.WildcardType wildcardType -> extractActualType(
                    wildcardType.type,
                    ((Type.WildcardType) genericOwner).type,
                    genericType
            );
            case Type.ArrayType arrayType -> extractActualType(
                    arrayType.getComponentType(),
                    ((Type.ArrayType) genericOwner).getComponentType(),
                    genericType
            );
            case Type.JCPrimitiveType primitiveType -> Optional.of(TypeUtils.getBoxedType(primitiveType));
            default -> Optional.empty();
        };
    }

    private boolean doesExistingArgsMatch(Type... argTypes) {
        var genericMapping = new HashMap<Name, Type>();
        return IntStream.range(0, argTypes.length)
                .peek(i ->
                        getGenericTypesIn(parameters.get(i).type).forEach(genericType -> {
                            if (genericTypeNames.contains(genericType.toString())) {
                                genericMapping.putIfAbsent(
                                        ExpressionMaker.makeName(genericType.toString()),
                                        extractActualType(
                                                argTypes[i],
                                                parameters.get(i).type,
                                                genericType
                                        ).orElseThrow()
                                );
                            }
                        }))
                .allMatch(i -> TypeUtils.isAssignable(
                        TypeUtils.getBoxedType(argTypes[i]),
                        TypeUtils.replaceAllTypeVars(parameters.get(i).type, genericMapping)
                ));
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
