package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;

public class IdentifierExpression extends NamedExpression {
    protected IdentifierExpression(String name) {
        super(name);
    }

    @Override
    public JCTree.JCExpression toExpression() {
        return ExpressionMaker.makeIdent(name);
    }
}
