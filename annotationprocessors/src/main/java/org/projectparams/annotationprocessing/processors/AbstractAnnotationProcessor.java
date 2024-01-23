package org.projectparams.annotationprocessing.processors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;

import javax.annotation.processing.Messager;
import java.lang.annotation.Annotation;

public abstract class AbstractAnnotationProcessor<T extends Annotation> implements AnnotationProcessor<T> {
    protected final Trees trees;
    protected final Messager messager;
    protected final TreeMaker treeMaker;

    protected AbstractAnnotationProcessor(Trees trees, TreeMaker treeMaker, Messager messager) {
        this.trees = trees;
        this.messager = messager;
        this.treeMaker = treeMaker;
    }
}
