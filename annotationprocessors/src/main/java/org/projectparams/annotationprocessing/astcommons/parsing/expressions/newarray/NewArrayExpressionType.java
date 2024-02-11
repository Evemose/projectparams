package org.projectparams.annotationprocessing.astcommons.parsing.expressions.newarray;

import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

import java.util.List;

public class NewArrayExpressionType extends AbstractExpressionType {

    private static final NewArrayExpressionType INSTANCE = new NewArrayExpressionType();

    public static NewArrayExpressionType getInstance() {
        return INSTANCE;
    }

    private NewArrayExpressionType() {}

    private static List<Expression> getInitializer(CreateExpressionParams createExpression,
                                                  int initializerStartIndex,
                                                  int dimsCount,
                                                  String type) {
        List<Expression> initializer = null;
        if (initializerStartIndex != -1) {
            initializer = ParsingUtils.getArrayInitializerExpressions(createExpression.expression())
                    .stream()
                    .map(init -> {
                        if (init.trim().matches("\\{.*}")) {
                            var arrayDims = dimsCount - 1;
                            if (arrayDims < 1) {
                                throw new IllegalArgumentException("Array initializer contains too many dimensions: "
                                        + init + " in " + createExpression.expression());
                            }
                            return ExpressionFactory.createExpression(createExpression.withExpressionAndNullTag(
                                    "new %s".formatted(type) + "[]".repeat(dimsCount-1) + init));
                        }
                        return ExpressionFactory.createExpression(createExpression.withExpressionAndNullTag(init));
                    }).toList();
        }
        return initializer;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression();
        var type = expression.substring(4, expression.indexOf('[')).strip();
        var initializerStartIndex = expression.indexOf('{');
        var dimensions = ExpressionUtils.getDimensions(createParams, initializerStartIndex);
        return new NewArrayExpression(type,
                dimensions,
                getInitializer(createParams, initializerStartIndex, dimensions.size(), type)
        );
    }

    @Override
    protected boolean matchesInner(String expression) {
        return expression.strip().matches("new\\s+[a-zA-Z_][\\w.$]*\\s*(\\[.*])+(\\s*\\{.*})?");
    }
}
