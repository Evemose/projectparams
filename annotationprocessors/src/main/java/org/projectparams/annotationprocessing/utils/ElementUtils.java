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

    public static Set<TypeElement> getAllAncestors(TypeElement typeElement) {
        var allClasses = new ArrayList<>(getAllClasses(typeElement));
        var result = new TreeSet<TypeElement>((a, b) -> {
            if (a.equals(b)) return 0;
            while (!a.toString().equals("java.lang.Object")
            && !a.getSuperclass().toString().equals("java.lang.Object")) {
                if (a.getSuperclass().equals(b.asType())) {
                    return -1;
                }
                a = elements.getTypeElement(a.getSuperclass().toString());
            }
            return 1;
        });
        result.add(typeElement);
        var previousSize = 0;
        do {
            previousSize = result.size();
            for (var clazz : allClasses) {
                if (result.contains(elements.getTypeElement(clazz.getSuperclass().toString()))) {
                    result.add(clazz);
                }
            }
            allClasses.removeAll(result);
        } while (result.size() > previousSize);
        return result;
    }

    public static List<TypeElement> getAllClasses(Element someElement) {
        var rootPackage = someElement;
        while (rootPackage.getEnclosingElement().getKind() != ElementKind.MODULE) {
            rootPackage = rootPackage.getEnclosingElement();
        }
        return getClassesInPackage((PackageElement) rootPackage);
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
