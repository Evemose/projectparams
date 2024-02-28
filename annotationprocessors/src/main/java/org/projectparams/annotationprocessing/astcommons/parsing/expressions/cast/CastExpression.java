package org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class CastExpression implements Expression {
    private final Expression castedExpression;
    private final String castedType;

    public CastExpression(Expression castedExpression, String castedType) {
        this.castedExpression = castedExpression;
        this.castedType = castedType;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeTypeCast(castedExpression.toJcExpression(), castedType);
    }

    @Override
    public void convertIdentsToQualified(ClassContext classContext) {
        castedExpression.convertIdentsToQualified(classContext);
    }
}
