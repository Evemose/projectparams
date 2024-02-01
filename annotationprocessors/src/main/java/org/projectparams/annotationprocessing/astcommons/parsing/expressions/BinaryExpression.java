package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class BinaryExpression implements Expression {
    private final Expression left;
    private final Expression right;
    private final JCTree.Tag operator;

    public BinaryExpression(Expression left, Expression right, JCTree.Tag operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeBinary(operator, left.toJcExpression(), right.toJcExpression());
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        left.convertInnerIdentifiersToQualified(classContext);
        right.convertInnerIdentifiersToQualified(classContext);
    }
}
