package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.memberref;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.Expression;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.selectable.fieldaccess.FieldAccessExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.List;

public class MemberReferenceExpression extends FieldAccessExpression {
    private final MemberReferenceTree.ReferenceMode mode;
    private final List<Expression> typeArgs;
    public MemberReferenceExpression(MemberReferenceTree.ReferenceMode mode, String name, Expression owner, List<Expression> typeArgs) {
        super(name, owner);
        this.mode = mode;
        this.typeArgs = typeArgs;
    }

    @Override
    public String toString() {
        return owner + "::" + name;
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeMemberReference(
                mode,
                owner.toJcExpression(),
                name,
                typeArgs == null ? null : typeArgs.stream().map(Expression::toJcExpression).toList()
        );
    }
}
