package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.newclass;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.InvocableExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.List;

public class NewClassExpression extends InvocableExpression {
    NewClassExpression(String name, Expression owner,
                       List<Expression> arguments,
                       TreePath enclosingInvocationPath,
                       List<Expression> typeParameters) {
        super(name, owner, arguments, enclosingInvocationPath, typeParameters);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        var expr = ExpressionMaker.makeNewClass(
                owner == null ? null : owner.toJcExpression(),
                name,
                typeParameters.stream().map(Expression::toJcExpression).toList(),
                arguments.stream().map(Expression::toJcExpression).toArray(JCTree.JCExpression[]::new));
        TypeUtils.attributeExpression(expr, new TreePath(enclosingInvocationPath, expr));
        return expr;
    }
}
