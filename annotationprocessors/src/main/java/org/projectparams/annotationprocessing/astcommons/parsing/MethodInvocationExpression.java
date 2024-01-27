package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;

import java.util.List;

public class MethodInvocationExpression extends InvocableExpression{
    protected MethodInvocationExpression(String name, Expression owner, List<Expression> arguments) {
        super(name, owner, arguments);
    }

    @Override
    public JCTree.JCExpression toExpression() {
        return ExpressionMaker.makeMethodInvocation(owner.toExpression(),
                arguments.stream().map(Expression::toExpression).toArray(JCTree.JCExpression[]::new));
    }
}
