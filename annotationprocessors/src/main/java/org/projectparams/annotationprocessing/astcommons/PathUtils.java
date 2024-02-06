package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;

public class PathUtils {
    private static Trees trees;
    public static void init(Trees trees) {
        PathUtils.trees = trees;
    }
    public static TreePath getEnclosingClassPath(TreePath path) {
        path = path.getParentPath();
        while (path != null && !(path.getLeaf() instanceof ClassTree)) {
            path = path.getParentPath();
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is not enclosed in class");
        }
        return path;
    }

    public static TreePath getElementPath(Element method) {
        return trees.getPath(method);
    }
}
