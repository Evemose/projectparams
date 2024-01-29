package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
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
        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR){
            messager.printMessage(Diagnostic.Kind.NOTE, "Error var: " + variable);
            var asJC = (JCTree.JCVariableDecl) variable;
            TypeUtils.attributeExpression(asJC, PathUtils.getEnclosingClassPath(getCurrentPath()).getLeaf());
            asJC.vartype = treeMaker.Type(((JCTree.JCMethodInvocation) variable.getInitializer()).type);
            asJC.type = asJC.vartype.type;
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed var: " + variable + " " +
                    ((JCTree.JCMethodInvocation) variable.getInitializer()).type);
        }
        return super.visitVariable(variable, ignored);
    }

}
