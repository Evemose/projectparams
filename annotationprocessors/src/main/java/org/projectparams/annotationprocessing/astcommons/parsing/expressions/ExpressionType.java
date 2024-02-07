package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

public interface ExpressionType {
    boolean matches(String expression);
    Expression parse(CreateExpressionParams createParams);
}
