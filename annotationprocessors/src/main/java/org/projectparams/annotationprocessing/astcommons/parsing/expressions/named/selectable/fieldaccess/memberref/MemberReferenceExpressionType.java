package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.memberref;

import com.sun.source.tree.MemberReferenceTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.AbstractExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.CreateExpressionParams;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.cast.CastExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.conditional.ConditionalExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.unary.UnaryExpressionType;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ParsingUtils;

public class MemberReferenceExpressionType extends AbstractExpressionType {
    private static final MemberReferenceExpressionType INSTANCE = new MemberReferenceExpressionType();

    private MemberReferenceExpressionType() {
    }

    public static MemberReferenceExpressionType getInstance() {
        return INSTANCE;
    }

    @Override
    public Expression parse(CreateExpressionParams createParams) {
        var expression = createParams.expression().strip();
        if (!matches(expression)) {
            throw new IllegalArgumentException("Expression " + expression + " does not match " + this.getClass().getSimpleName());
        }
        var parts = expression.split("::");
        var owner = ExpressionFactory.createExpression(createParams.withExpressionAndNullTag(parts[0]));
        var typeArgsIndex = ParsingUtils.getTypeArgsStartIndex(parts[1]);
        if (typeArgsIndex != -1) {
            return new MemberReferenceExpression(
                    parts[1].equals("new") ? MemberReferenceTree.ReferenceMode.NEW : MemberReferenceTree.ReferenceMode.INVOKE,
                    parts[1].equals("new") ? "<init>" : parts[1].substring(parts[1].lastIndexOf('>') + 1),
                    owner,
                    ExpressionUtils.getTypeArgs(createParams));
        } else {
            return new MemberReferenceExpression(
                    parts[1].equals("new") ? MemberReferenceTree.ReferenceMode.NEW : MemberReferenceTree.ReferenceMode.INVOKE,
                    parts[1].equals("new") ? "<init>" : parts[1],
                    owner,
                    null);
        }
    }

    @Override
    protected boolean matchesInner(String expression) {
        return expression.matches(".*::(<(\\w[\\w,$\\s]*)*>)?\\w[\\w$]*");
    }

    @Override
    protected boolean isCovered(String expression) {
        return UnaryExpressionType.getInstance().matches(expression)
                || BinaryExpressionType.getInstance().matches(expression)
                || ConditionalExpressionType.getInstance().matches(expression)
                || CastExpressionType.getInstance().matches(expression);
    }
}
