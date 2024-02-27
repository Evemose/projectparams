package org.projectparams.annotationprocessing.utils;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ElementUtils {
    private static Elements elements;
    private static PackageElement rootPackage;
    private static Trees trees;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Elements elements, PackageElement rootPackage, Trees trees) {
        ElementUtils.elements = elements;
        ElementUtils.rootPackage = rootPackage;
        ElementUtils.trees = trees;
    }

    public static PackageElement getPackageByName(String packageName) {
        return elements.getPackageElement(packageName);
    }

    public static TypeElement getClassByName(String className) {
        return elements.getTypeElement(className);
    }

    public static List<TypeElement> getAllChildren(TypeElement typeElement) {
        return getAllClasses(typeElement).stream()
                .filter(clazz -> TypeUtils.isAssignable((Type) clazz.asType(), (Type) typeElement.asType())
                && !clazz.equals(typeElement))
                .toList();
    }

    public static List<TypeElement> getAllClasses(Element someElement) {
        var rootPackage = someElement;
        while (rootPackage.getEnclosingElement().getKind() != ElementKind.MODULE) {
            rootPackage = rootPackage.getEnclosingElement();
        }
        return getClassesInPackage((PackageElement) rootPackage);
    }

    public static List<TypeElement> getAllTopLevelClasses(Element someElement) {
        return getAllClasses(someElement).stream()
                .filter(clazz -> !clazz.getNestingKind().isNested())
                .toList();
    }

    public static List<TypeElement> getClassesInPackage(PackageElement packageElement) {
        return packageElement.getEnclosedElements().stream()
                .flatMap(e -> {
                    if (e.getKind() == ElementKind.CLASS) {
                        return getAllClassesInClass((TypeElement) e).stream();
                    } else if (e.getKind() == ElementKind.PACKAGE) {
                        return getClassesInPackage((PackageElement) e).stream();
                    } else {
                        return Stream.of();
                    }
                })
                .toList();
    }

    public static List<TypeElement> getAllClassesInClass(TypeElement classElement) {
        var result = new ArrayList<>(List.of(classElement));
        result.addAll(classElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.CLASS)
                .flatMap(e -> getAllClassesInClass((TypeElement) e).stream())
                .toList());
        return result;
    }

    public static PackageElement getRootPackage() {
        return rootPackage;
    }

    public static Element getClassByPath(TreePath classPath) {
        return trees.getElement(classPath);
    }

    public static TreePath getPath(ExecutableElement method) {
        return trees.getPath(method);
    }
}
