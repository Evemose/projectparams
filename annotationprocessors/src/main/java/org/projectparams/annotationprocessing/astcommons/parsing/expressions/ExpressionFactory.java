package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal.LiteralExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public class ExpressionFactory {

    public static Messager messager;

    private ExpressionFactory() {
    }

    public static Expression createExpression(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        messager.printMessage(Diagnostic.Kind.NOTE, "Creating expression from " + expression);
        if (expression == null) {
            return LiteralExpression.NULL;
        }
        var type = ExpressionUtils.getType(createParams);
        messager.printMessage(Diagnostic.Kind.NOTE, "Type of expression: " + type);
        return type.parse(createParams);
    }
}
