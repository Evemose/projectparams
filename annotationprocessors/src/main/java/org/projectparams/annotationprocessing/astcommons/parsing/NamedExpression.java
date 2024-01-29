package org.projectparams.annotationprocessing.astcommons.parsing;

public abstract class NamedExpression implements Expression {
    protected final String name;

    public String name() {
        return name;
    }

    protected NamedExpression(String name) {
        this.name = name;
    }
}
