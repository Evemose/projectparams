package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class FieldAccessExpression extends SelectableExpression {
    protected FieldAccessExpression(String name, Expression owner) {
        super(name, owner);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeFieldAccess(owner.toJcExpression(), name);
    }
}
