package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class UnaryExpression implements Expression{
    private final Expression expression;
    private final JCTree.Tag operator;

    public UnaryExpression(Expression expression, JCTree.Tag operator) {
        this.expression = expression;
        this.operator = operator;
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
