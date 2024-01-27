package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;

public class FieldAccessExpression extends SelectableExpression {
    protected FieldAccessExpression(String name, Expression owner) {
        super(name, owner);
    }

    @Override
    public JCTree.JCExpression toExpression() {
        return ExpressionMaker.makeFieldAccess(owner.toExpression(), name);
    }
}
