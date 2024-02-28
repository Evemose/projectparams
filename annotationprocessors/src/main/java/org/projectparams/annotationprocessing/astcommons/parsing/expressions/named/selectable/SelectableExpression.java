package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable;

import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.NamedExpression;

public abstract class SelectableExpression extends NamedExpression {
    protected final Expression owner;

    protected SelectableExpression(String name, Expression owner) {
        super(name);
        this.owner = owner;
    }

    @Override
    public void convertIdentsToQualified(ClassContext classContext) {
        if (owner != null) {
            owner.convertIdentsToQualified(classContext);
        }
    }
}
