package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import org.projectparams.annotationprocessing.astcommons.parsing.ExpressionType;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractExpressionType implements ExpressionType {
    private final Map<String, Boolean> matchesCache = new HashMap<>();
    protected boolean canMatchNulls = false;

    @Override
    public boolean matches(String expression) {
        if (matchesCache.containsKey(expression)) {
            return matchesCache.get(expression);
        }
        var result = (expression != null || canMatchNulls) && !isCovered(expression) && matchesInner(expression);
        matchesCache.put(expression, result);
        return result;
    }

    protected abstract boolean matchesInner(String expression);

    protected boolean isCovered(String expression) {
        return false;
    }
}
