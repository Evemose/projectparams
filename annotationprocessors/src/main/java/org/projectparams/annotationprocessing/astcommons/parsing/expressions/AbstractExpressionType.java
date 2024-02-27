package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import org.projectparams.annotationprocessing.astcommons.parsing.ExpressionType;

public abstract class AbstractExpressionType implements ExpressionType {
    @Override
    public boolean matches(String expression) {
        return !isCovered(expression) && matchesInner(expression);
    }

    protected abstract boolean matchesInner(String expression);

    protected boolean isCovered(String expression) {
        return false;
    }
}
