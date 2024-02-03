package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class ParenthesizedExpression implements Expression {
    private final Expression expression;

    public ParenthesizedExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeParens(expression.toJcExpression());
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        expression.convertInnerIdentifiersToQualified(classContext);
    }
}