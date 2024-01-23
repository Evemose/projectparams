package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import javax.annotation.processing.Messager;

public abstract class AbstractVisitor<R, P> extends TreePathScanner<R, P> {
    protected final Trees trees;
    protected final Messager messager;

    public AbstractVisitor(Trees trees, Messager messager) {
        this.trees = trees;
        this.messager = messager;
    }
}
