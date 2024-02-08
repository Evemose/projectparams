package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;

public abstract class OperatorExpression implements Expression {
    protected final JCTree.Tag operator;

    public OperatorExpression(JCTree.Tag operator) {
        this.operator = operator;
    }
}
