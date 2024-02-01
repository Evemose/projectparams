package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

public class IdentifierExpression extends NamedExpression {
    protected IdentifierExpression(String name) {
        super(name);
    }

    @Override
    public JCTree.JCExpression toJcExpression() {
        return ExpressionMaker.makeIdent(name);
    }

    @Override
    public void convertInnerIdentifiersToQualified(ClassContext classContext) {
        var matchingField = classContext.getMatchingField(name);
        if (matchingField.isPresent()) {
            name = (matchingField.get().isStatic() ? matchingField.get().className() + '.' : "this.") + name;
        } else {
            var matchingImportIdent = classContext.cuContext().getMatchingImportedOrStaticClass(name);
            matchingImportIdent.ifPresent(s -> name = s);
        }
    }

}
