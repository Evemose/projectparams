package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class TernaryExpression implements Expression {
    private final Expression condition;
    private final Expression ifTrue;
    private final Expression ifFalse;

    public TernaryExpression(Expression condition, Expression ifTrue, Expression ifFalse) {
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeConditional(
                condition.toJcExpression(),
                ifTrue.toJcExpression(),
                ifFalse.toJcExpression());
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        condition.convertInnerIdentifiersToQualified(classContext);
        ifTrue.convertInnerIdentifiersToQualified(classContext);
        ifFalse.convertInnerIdentifiersToQualified(classContext);
    }
}
