package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;


import java.util.List;

public class MethodInvocationExpression extends InvocableExpression {
    public MethodInvocationExpression(String name,
                                         Expression owner,
                                         List<Expression> arguments,
                                         TreePath enclosingInvocationPath) {
        super(name, owner, arguments, enclosingInvocationPath);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeMethodInvocation(
                ExpressionMaker.makeFieldAccess(getOwnerExpression(), name),
                arguments.stream().map(Expression::toJcExpression).toArray(JCTree.JCExpression[]::new));
    }

    private JCTree.JCExpression getOwnerExpression() {
        if (owner == null) {
            var classContext = ClassContext.classMember(PathUtils.getEnclosingClassPath(enclosingInvocationPath));
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
