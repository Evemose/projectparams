package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import java.util.List;

public class NewClassExpression extends InvocableExpression{
    protected NewClassExpression(String name, Expression owner,
                                 List<Expression> arguments,
                                 TreePath enclosingInvocationPath) {
        super(name, owner, arguments, enclosingInvocationPath);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        var expr =  ExpressionMaker.makeNewClass(owner == null ? null : owner.toJcExpression(),
                name,
                arguments.stream()
                .map(Expression::toJcExpression)
                .toArray(JCTree.JCExpression[]::new));
        TypeUtils.attributeExpression(expr, enclosingInvocationPath);
        return expr;
    }
}
