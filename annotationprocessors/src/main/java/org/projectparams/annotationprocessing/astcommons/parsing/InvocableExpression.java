package org.projectparams.annotationprocessing.astcommons.parsing;

import java.util.List;

public abstract class InvocableExpression extends SelectableExpression {
    protected final List<Expression> arguments;

    protected InvocableExpression(String name, Expression owner, List<Expression> arguments) {
        super(name, owner);
        this.arguments = arguments;
    }
}
