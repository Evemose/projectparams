package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.CUContext;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;


import javax.annotation.processing.Messager;
import java.util.List;

public class MethodInvocationExpression extends InvocableExpression {
    public static Messager messager;
    public MethodInvocationExpression(String name,
                                         Expression owner,
                                         List<Expression> arguments,
                                         TreePath enclosingInvocationPath) {
        super(name, owner, arguments, enclosingInvocationPath);
    }

    @Override
    public JCTree.JCExpression toExpression() {
        var invocation = ExpressionMaker.makeMethodInvocation(
                ExpressionMaker.makeFieldAccess(getOwnerExpression(), name),
                arguments.stream().map(Expression::toExpression).toArray(JCTree.JCExpression[]::new));
        TypeUtils.attributeExpression(invocation,
                TypeUtils.getEnclosingClassPath(enclosingInvocationPath).getLeaf());
        messager.printMessage(javax.tools.Diagnostic.Kind.NOTE,
                "Attributed invocation: " + invocation);
        return invocation;
    }

    private JCTree.JCExpression getOwnerExpression() {
        if (owner == null) {
            var classContext = ClassContext.from(TypeUtils.getEnclosingClassPath(enclosingInvocationPath));
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
            return owner.toExpression();
        }
    }
}
