package org.projectparams.annotationprocessing.utils;

public class NameUtils {
    private NameUtils() {
        throw new UnsupportedOperationException();
    }

    public static String toClassFileName(String className) {
        return className.replace('.', '/') + ".class";
    }
}
