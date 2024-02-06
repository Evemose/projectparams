package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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
                dimensions.stream().map(expr -> expr == null ? null : expr.toJcExpression()).toList(),
                initializers == null ? null : initializers.stream().map(Expression::toJcExpression).toList()
        );
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        dimensions.stream().filter(Objects::nonNull).forEach(expr -> expr.convertInnerIdentifiersToQualified(classContext));
        if (initializers != null) {
            initializers.forEach(expr -> expr.convertInnerIdentifiersToQualified(classContext));
        }
    }
}
