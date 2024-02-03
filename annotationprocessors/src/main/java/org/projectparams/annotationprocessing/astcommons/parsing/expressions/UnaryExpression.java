package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class UnaryExpression extends OperatorExpression {
    private final Expression expression;

    public UnaryExpression(Expression expression, JCTree.Tag operator) {
        super(operator);
        this.expression = expression;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeUnary(operator, expression.toJcExpression());
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        expression.convertInnerIdentifiersToQualified(classContext);
    }
}
