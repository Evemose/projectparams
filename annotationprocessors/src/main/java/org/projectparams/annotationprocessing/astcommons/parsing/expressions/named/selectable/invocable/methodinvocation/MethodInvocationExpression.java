package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.methodinvocation;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.invocable.InvocableExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;


import java.util.List;

public class MethodInvocationExpression extends InvocableExpression {
    public MethodInvocationExpression(String name,
                                         Expression owner,
                                         List<Expression> arguments,
                                         TreePath enclosingInvocationPath,
                                         List<Expression> typeParameters) {
        super(name, owner, arguments, enclosingInvocationPath, typeParameters);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeMethodInvocation(
                ExpressionMaker.makeFieldAccess(getOwnerExpression(), name),
                typeParameters.stream().map(Expression::toJcExpression).toList(),
                arguments.stream().map(Expression::toJcExpression).toArray(JCTree.JCExpression[]::new)
        );
    }

    private JCTree.JCExpression getOwnerExpression() {
        if (owner == null) {
            var classContext = ClassContext.of(PathUtils.getEnclosingClassPath(enclosingInvocationPath));
            var matchingMethod = classContext.getMatchingMethod(name);
            if (matchingMethod.isEmpty()) {
                throw new IllegalArgumentException("No matching method found for " + name);
            }
            var method = matchingMethod.get();
            if (method.isStatic()) {
                return ExpressionMaker.makeIdent(method.className());
            } else {
                return ExpressionMaker.makeIdent("this");
            }
        } else {
            return owner.toJcExpression();
        }
    }
}
