package org.projectparams.processors.ast.utils;

import javax.lang.model.element.Element;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.util.Optional;

public class ElementUtils {
    private ElementUtils() {
        throw new UnsupportedOperationException();
    }
    public static Optional<JCCompilationUnit> getCompilationUnit(Trees applicationTree, Element element) {
        try {
            var elementPath = applicationTree.getPath(element);
            if (elementPath == null) return Optional.empty();
            return Optional.of((JCCompilationUnit) elementPath.getCompilationUnit());
        } catch (NullPointerException ignored) {
            // can be ignored, happens when package-info.java does not have a package declaration
            // (at least that what lombok says)
        }
        return Optional.empty();
    }


}
