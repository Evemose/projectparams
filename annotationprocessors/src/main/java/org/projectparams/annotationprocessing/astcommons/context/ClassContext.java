package org.projectparams.annotationprocessing.astcommons.context;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;
import org.projectparams.annotationprocessing.utils.ElementUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import java.util.Optional;
import java.util.Set;

public record ClassContext(
        TreePath classPath,
        CUContext cuContext,
        Set<Method> methods,
        Set<Field> fields
) {
    public interface ClassMember {
        String name();
        String className();
        boolean isStatic();
    }
    public record Method (
            String name,
            String className,
            boolean isStatic
    ) implements ClassMember {}

    public record Field(
            String name,
            String className,
            boolean isStatic
    ) implements ClassMember {}

    public static ClassMember of(String name, String className, boolean isStatic, ElementKind kind) {
        return switch (kind) {
            case METHOD -> new Method(name, className, isStatic);
            case FIELD -> new Field(name, className, isStatic);
            default -> throw new IllegalArgumentException("Unexpected value: " + kind);
        };
    }

    public static ClassContext of(TreePath classPath) {
        if (!(classPath.getLeaf() instanceof com.sun.source.tree.ClassTree)) {
            throw new IllegalArgumentException("Expected class, got " + classPath.getLeaf().getKind());
        }
        var cuContext = CUContext.from(classPath.getCompilationUnit());
        var methods = ContextUtils.getMethodsInClass(classPath);
        var fields = ContextUtils.getFieldsInClass(classPath);
        return new ClassContext(classPath, cuContext, methods, fields);
    }

    public String getClassName() {
        return ((TypeElement)ElementUtils.getClassByPath(classPath)).getQualifiedName().toString();
    }

    public Optional<Method> getMatchingMethod(String methodName) {
        var matchingMethod = methods.stream()
                .filter(method -> method.name.equals(methodName))
                .findAny();
        if (matchingMethod.isEmpty()) {
            matchingMethod = cuContext.getMatchingImportedStaticMethod(methodName)
                    .map(fullName -> {
                        var className = fullName.substring(0, fullName.length() - methodName.length() - 1);
                        return new Method(methodName, className, true);
                    });
        }
        return matchingMethod;
    }

    public Optional<Field> getMatchingField(String fieldName) {
        return fields.stream()
                .filter(field -> field.name.equals(fieldName))
                .findAny();
    }
}
