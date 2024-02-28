package org.projectparams.annotationprocessing.astcommons.parsing.expressions.literal;

import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;

public class LiteralExpressionType extends AbstractExpressionType {

    private static final LiteralExpressionType INSTANCE = new LiteralExpressionType();

    {
        canMatchNulls = true;
    }

    private LiteralExpressionType() {
    }

    public static LiteralExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        var typeTag = createParams.typeTag();
        if (typeTag == null) {
            typeTag = TypeUtils.geLiteralTypeTag(expression);
        }
        var value = TypeUtils.literalValueFromStr(typeTag, expression);
        return new LiteralExpression(value);
    }

    @Override
    protected boolean matchesInner(String expression) {
        if (expression == null) {
            return true;
        }
        return expression.strip().matches("(?s)(((\\d+(\\.\\d*)?)|(\\.\\d*))([fdlFDL]|([eE][+-]?\\d+[fdFD]?))?)" +
                "|(true|false)|('.')|(\".*\")|((0[xX][0-9a-fA-F]+)|(0[bB][01]+)|(0[0-7]+)[dDlLfF]?)")
                && !expression.matches("(\\d+\\.){2,}\\d+");
    }
}
