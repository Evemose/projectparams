package org.projectparams.annotationprocessing.astcommons.parsing.expressions.lambda;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.List;

public class LambdaExpression implements Expression {
    private final List<String> parameters;
    private final Expression body;

    public LambdaExpression(List<String> parameters, Expression body) {
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeLambda(
                parameters.stream()
                        .map(arg -> ExpressionMaker.makeVariableDecl(arg, null)).toList(),
                body.toJcExpression());
    }

    @Override
    public void convertIdentsToQualified(ClassContext classContext) {
        body.convertIdentsToQualified(classContext);
    }
}
