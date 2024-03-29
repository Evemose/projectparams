package org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class ConditionalExpression implements Expression {
    private final Expression condition;
    private final Expression ifTrue;
    private final Expression ifFalse;

    public ConditionalExpression(Expression condition, Expression ifTrue, Expression ifFalse) {
        this.condition = condition;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeConditional(
                condition.toJcExpression(),
                ifTrue.toJcExpression(),
                ifFalse.toJcExpression()
        );
    }

    @Override
    public void convertIdentsToQualified(ClassContext classContext) {
        condition.convertIdentsToQualified(classContext);
        ifTrue.convertIdentsToQualified(classContext);
        ifFalse.convertIdentsToQualified(classContext);
    }
}
