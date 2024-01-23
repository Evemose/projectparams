package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.Messager;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;

public class PackageTree {
    private final List<TypeElement> classes;
    private final Messager messager;
    private final Trees trees;

    public PackageTree(PackageElement packageDecl, Messager messager, Trees trees) {
        classes = new ArrayList<>();
        this.messager = messager;
        this.trees = trees;
        addPackageClasses(packageDecl);
    }

    private void addPackageClasses(PackageElement packageDecl) {
        for (var element : packageDecl.getEnclosedElements()) {
            if (element instanceof PackageElement) {
                addPackageClasses((PackageElement) element);
            } else if (element instanceof TypeElement) {
                classes.add((TypeElement) element);
            }
        }
    }

    public <R, P> void accept(TreePathScanner<R, P> scanner, P arg) {
        classes.forEach(clazz -> {
            scanner.scan(trees.getPath(clazz), arg);
        });
    }
}
