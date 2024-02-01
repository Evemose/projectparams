package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import java.util.List;

public class NewClassExpression extends InvocableExpression{
    protected NewClassExpression(String name, Expression owner,
                                 List<Expression> arguments,
                                 TreePath enclosingInvocationPath,
                                 List<Expression> typeParameters) {
        super(name, owner, arguments, enclosingInvocationPath, typeParameters);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        var expr =  ExpressionMaker.makeNewClass(
                owner == null ? null : owner.toJcExpression(),
                name,
                typeParameters.stream().map(Expression::toJcExpression).toList(),
                arguments.stream().map(Expression::toJcExpression).toArray(JCTree.JCExpression[]::new));
        TypeUtils.attributeExpression(expr, enclosingInvocationPath);
        return expr;
    }
}
