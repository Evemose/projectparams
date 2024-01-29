package org.projectparams.annotationprocessing.processors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.PackageTree;

import javax.annotation.processing.Messager;
import javax.lang.model.element.PackageElement;
import java.lang.annotation.Annotation;

/**
 * Annotation processor that processes all compilation units in the project.
 *
 * @param <T> Annotation that this processor processes.
 */
public abstract class GlobalAnnotationProcessor<T extends Annotation> extends AbstractAnnotationProcessor<T> {
    protected final PackageTree packageTree;

    public GlobalAnnotationProcessor(Trees trees, TreeMaker treeMaker, PackageElement rootPackageElement, Messager messager) {
        super(trees, treeMaker, messager);
        this.packageTree = PackageTree.ofRoot(trees);
    }
}
