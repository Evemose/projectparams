package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.processing.Messager;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;

public class PostModificationAttributionVisitor extends AbstractVisitor<Void, Void> {
    private final TreeMaker treeMaker;

    public PostModificationAttributionVisitor(TreeMaker treeMaker, Trees trees, Messager messager) {
        super(trees, messager);
        this.treeMaker = treeMaker;
    }

    @Override
    public Void visitVariable(VariableTree variable, Void ignored) {
        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Error var: " + variable);
            var asJC = (JCTree.JCVariableDecl) variable;
            if (asJC.init == null) {
                return super.visitVariable(variable, ignored);
            }
            TypeUtils.attributeExpression(asJC, getCurrentPath());
            asJC.vartype = treeMaker.Type(TypeUtils.getActualType(variable.getInitializer()));
            asJC.type = asJC.vartype.type;
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed var: " + variable + " " +
                    TypeUtils.getActualType(variable.getInitializer()));
        }
        return super.visitVariable(variable, ignored);
    }

}
