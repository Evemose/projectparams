package org.projectparams.annotationprocessing.astcommons.parsing;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;

public interface ExpressionType {
    boolean matches(String expression);

    Expression parse(CreateExpressionParams createParams);
}
