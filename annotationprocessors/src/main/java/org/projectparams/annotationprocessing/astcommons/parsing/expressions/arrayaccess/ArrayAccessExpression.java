package org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class ArrayAccessExpression implements Expression {
    private final Expression array;
    private final Expression index;
    public ArrayAccessExpression(Expression array, Expression index) {
        this.array = array;
        this.index = index;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeArrayAccess(array.toJcExpression(), index.toJcExpression());
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        array.convertInnerIdentifiersToQualified(classContext);
        index.convertInnerIdentifiersToQualified(classContext);
    }
}
