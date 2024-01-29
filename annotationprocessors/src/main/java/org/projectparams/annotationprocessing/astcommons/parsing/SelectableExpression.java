package org.projectparams.annotationprocessing.astcommons.parsing;

public abstract class SelectableExpression extends NamedExpression {
    protected final Expression owner;

    protected SelectableExpression(String name, Expression owner) {
        super(name);
        this.owner = owner;
    }

    @Override
    public Expression getRootOwner() {
        return owner.getRootOwner();
    }
}
