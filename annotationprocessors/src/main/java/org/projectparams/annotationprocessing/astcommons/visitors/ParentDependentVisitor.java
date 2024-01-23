package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;

import javax.annotation.processing.Messager;

/**
 * An abstract class that represents a visitor pattern implementation where the behavior of the visitor
 * depends on the parent tree node. Parent aren`t always direct parent node but rather the closest parent node of certain type.
 *
 * @param <R> The type of the result returned by the visitor.
 * @param <P> The type of the parameter passed to the visitor.
 * @param <T> The type of the parent node.
 */
public abstract class ParentDependentVisitor<R, P, T extends Tree> extends AbstractVisitor<R, P> {
    protected T parent;

    public ParentDependentVisitor(Trees trees, Messager messager, TreeMaker treeMaker, T parent) {
        super(trees, messager, treeMaker);
        this.parent = parent;
    }
}
