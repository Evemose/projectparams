package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import org.projectparams.annotationprocessing.utils.ElementUtils;

import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class PackageTree {
    private final List<TypeElement> classes;
    private final Trees trees;

    public PackageTree(PackageElement packageDecl, Trees trees) {
        classes = ElementUtils.getAllTopLevelClasses(packageDecl);
        this.trees = trees;
    }

    public <R, P> void accept(TreePathScanner<R, P> scanner, P arg) {
        classes.forEach(clazz -> {
            scanner.scan(trees.getPath(clazz), arg);
        });
    }
}
