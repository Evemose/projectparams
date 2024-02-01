package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

public abstract class NamedExpression implements Expression {

    // not final because can be converted to qualified
    protected String name;

    public String name() {
        return name;
    }

    protected NamedExpression(String name) {
        this.name = name;
    }
}
