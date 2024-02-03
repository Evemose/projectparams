package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;

public abstract class OperatorExpression implements Expression{
    protected final JCTree.Tag operator;

    public OperatorExpression(JCTree.Tag operator) {
        this.operator = operator;
    }
}
