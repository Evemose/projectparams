package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.ident;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.NamedExpression;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class IdentifierExpression extends NamedExpression {
    public IdentifierExpression(String name) {
        super(name);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeIdent(name);
    }

    @Override
    public void convertIdentsToQualified(ClassContext classContext) {
        var matchingField = classContext.getMatchingField(name);
        if (matchingField.isPresent()) {
            name = (matchingField.get().isStatic() ? matchingField.get().className() + '.' : "this.") + name;
        } else {
            var matchingImportIdent = classContext.cuContext().getMatchingImportedOrStaticClass(name);
            matchingImportIdent.ifPresent(s -> name = s);
        }
    }

}
