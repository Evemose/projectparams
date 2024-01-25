package org.projectparams.annotationprocessing.utils;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public class ElementUtils {
    private static Elements elements;
    private static Trees trees;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Elements elements, Trees trees) {
        ElementUtils.elements = elements;
        ElementUtils.trees = trees;
    }

    public static PackageElement getPackageByName(String packageName) {
        return elements.getPackageElement(packageName);
    }

    public static TypeElement getClassByName(String className) {
        return elements.getTypeElement(className);
    }

    public static Element getElementByPath(TreePath path) {
        return trees.getElement(path);
    }

    public static List<TypeElement> getAllChildren(TypeElement typeElement) {
        var allClasses = new ArrayList<>(getAllClasses(typeElement));
        var result = new ArrayList<TypeElement>();
        result.add(typeElement);
        allClasses.remove(typeElement);
        int previousSize;
        do {
            previousSize = result.size();
            for (var clazz : allClasses) {
                if (result.contains(elements.getTypeElement(clazz.getSuperclass().toString()))) {
                    result.add(clazz);
                }
            }
            allClasses.removeAll(result);
        } while (result.size() > previousSize);
        result.remove(typeElement);
        return result;
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
                .map(e -> (TypeElement) e)
                .toList());
        return result;
    }
}
