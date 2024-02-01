package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import org.projectparams.annotationprocessing.astcommons.context.ClassContext;

public abstract class SelectableExpression extends NamedExpression {
    protected final Expression owner;

    protected SelectableExpression(String name, Expression owner) {
        super(name);
        this.owner = owner;
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        if (owner != null) {
            owner.convertInnerIdentifiersToQualified(classContext);
        }
    }
}
