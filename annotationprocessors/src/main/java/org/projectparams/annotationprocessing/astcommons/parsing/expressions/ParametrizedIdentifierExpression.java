package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;

import java.util.List;

public class ParametrizedIdentifierExpression extends IdentifierExpression implements ParameterizableExpression{
    private final ParametrizedExpression parametrizedExpression;
    protected ParametrizedIdentifierExpression(String name, List<Expression> typeArguments) {
        super(name);
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
