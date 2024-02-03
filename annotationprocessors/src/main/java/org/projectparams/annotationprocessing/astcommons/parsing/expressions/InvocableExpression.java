package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.source.util.TreePath;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;

import java.util.List;

public abstract class InvocableExpression extends SelectableExpression {
    protected final List<Expression> arguments;
    protected final TreePath enclosingInvocationPath;
    protected final List<Expression> typeParameters;

    protected InvocableExpression(String name, Expression owner, List<Expression> arguments, TreePath enclosingInvocationPath, List<Expression> typeParameters) {
        super(name, owner);
        this.arguments = arguments;
        this.enclosingInvocationPath = enclosingInvocationPath;
        this.typeParameters = typeParameters;
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        super.convertInnerIdentifiersToQualified(classContext);
        arguments.forEach(arg -> arg.convertInnerIdentifiersToQualified(classContext));
    }
}
