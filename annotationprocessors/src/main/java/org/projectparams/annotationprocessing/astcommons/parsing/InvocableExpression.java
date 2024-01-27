package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.util.TreePath;

import java.util.List;

public abstract class InvocableExpression extends SelectableExpression {
    protected final List<Expression> arguments;
    protected final TreePath enclosingInvocationPath;

    protected InvocableExpression(String name, Expression owner, List<Expression> arguments, TreePath enclosingInvocationPath) {
        super(name, owner);
        this.arguments = arguments;
        this.enclosingInvocationPath = enclosingInvocationPath;
    }
}
