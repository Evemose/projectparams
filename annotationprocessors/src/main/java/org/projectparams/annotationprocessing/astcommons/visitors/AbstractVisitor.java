package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;

import javax.annotation.processing.Messager;

public abstract class AbstractVisitor<R, P> extends TreePathScanner<R, P> {
    protected final Trees trees;
    protected final Messager messager;
    protected final TreeMaker treeMaker;

    public AbstractVisitor(Trees trees, Messager messager, TreeMaker treeMaker) {
        this.trees = trees;
        this.messager = messager;
        this.treeMaker = treeMaker;
    }
}
