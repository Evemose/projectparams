package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.List;

public class NewArrayExpression implements Expression {
    private final String type;
    private final List<Expression> dimensions;
    private final List<Expression> initializers;

    public NewArrayExpression(String type, List<Expression> dimensions, List<Expression> initializers) {
        this.type = type;
        this.dimensions = dimensions;
        this.initializers = initializers;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeNewArray(
                type,
                dimensions.stream().map(Expression::toJcExpression).toList(),
                initializers.stream().map(Expression::toJcExpression).toList()
        );
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {

    }
}
