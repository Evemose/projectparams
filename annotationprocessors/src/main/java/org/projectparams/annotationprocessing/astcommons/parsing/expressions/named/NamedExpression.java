package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;

public abstract class NamedExpression implements Expression {

    // not final because can be converted to qualified
    protected String name;

    public String name() {
        return name;
    }

    protected NamedExpression(String name) {
        this.name = name.trim();
    }
}
