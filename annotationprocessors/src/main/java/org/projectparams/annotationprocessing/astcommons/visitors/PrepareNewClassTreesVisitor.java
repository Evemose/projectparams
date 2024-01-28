package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.processing.Messager;

public class PrepareNewClassTreesVisitor extends AbstractVisitor<Void, Void> {
    public PrepareNewClassTreesVisitor(Trees trees, Messager messager) {
        super(trees, messager);
    }

    @Override
    public Void visitNewClass(NewClassTree newClassTree, Void aVoid) {
        var asJC = (JCTree.JCNewClass) newClassTree;
        var enclosingExpression = asJC.getEnclosingExpression();
        if ((enclosingExpression == null || enclosingExpression.type != null)
        && (asJC.getIdentifier().type != null
                && !asJC.getIdentifier().type.tsym.isEnum())) {
            TypeUtils.attributeExpression(asJC,
                    TypeUtils.getEnclosingClassPath(getCurrentPath()).getLeaf());
            messager.printMessage(javax.tools.Diagnostic.Kind.NOTE, "Enclosing expression is null for "
                    + newClassTree + " Identifier: " + asJC.getIdentifier().type);
        }
        return super.visitNewClass(newClassTree, aVoid);
    }
}
