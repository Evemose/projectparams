package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.List;

public class ParametrizedExpression implements Expression {
    private final List<Expression> typeArguments;
    private final ParameterizableObjectExpression plainExpression;
    protected ParametrizedExpression(List<Expression> typeArguments, ParameterizableObjectExpression plainExpression) {
        this.typeArguments = typeArguments;
        this.plainExpression = plainExpression;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        var plainExpression = this.plainExpression.superToJcExpression();
        var typeArguments =
                this.typeArguments.stream().map(Expression::toJcExpression).toArray(JCTree.JCExpression[]::new);
        return ExpressionMaker.makeTypeApply(plainExpression, typeArguments);
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        typeArguments.forEach(typeArgument -> typeArgument.convertInnerIdentifiersToQualified(classContext));
    }
}
