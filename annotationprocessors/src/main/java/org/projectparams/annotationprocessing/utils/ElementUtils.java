package org.projectparams.annotationprocessing.utils;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public class ElementUtils {
    private static Elements elements;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Elements elements) {
        ElementUtils.elements = elements;
    }

    public static PackageElement getPackageByName(String packageName) {
        return elements.getPackageElement(packageName);
    }

    public static TypeElement getClassByName(String className) {
        return elements.getTypeElement(className);
    }
}
