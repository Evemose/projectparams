package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import java.util.List;

public class ParametrizedFieldAccessExpression extends FieldAccessExpression implements ParameterizableExpression{
    private final ParametrizedExpression parametrizedExpression;
    protected ParametrizedFieldAccessExpression(String name, Expression owner, List<Expression> typeArguments) {
        super(name, owner);
        this.parametrizedExpression = new ParametrizedExpression(typeArguments, this);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return parametrizedExpression.toJcExpression();
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        super.convertInnerIdentifiersToQualified(classContext);
        parametrizedExpression.convertInnerIdentifiersToQualified(classContext);
    }

    @Override
    public JCTree.JCExpression superToJcExpression() {
        return super.toJcExpression();
    }
}
