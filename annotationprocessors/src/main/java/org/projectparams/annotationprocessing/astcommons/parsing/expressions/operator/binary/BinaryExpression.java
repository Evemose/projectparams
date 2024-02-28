package org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.OperatorExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class BinaryExpression extends OperatorExpression {
    private final Expression left;
    private final Expression right;

    public BinaryExpression(Expression left, Expression right, JCTree.Tag operator) {
        super(operator);
        this.left = left;
        this.right = right;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeBinary(operator, left.toJcExpression(), right.toJcExpression());
    }

    @Override
    public void convertIdentsToQualified(ClassContext classContext) {
        left.convertIdentsToQualified(classContext);
        right.convertIdentsToQualified(classContext);
    }
}
