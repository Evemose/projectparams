package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;

import javax.annotation.processing.Messager;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.Set;

public class CleanupVisitor extends AbstractVisitor<Void, Void> {
    private final Set<InvocableTree> allFixedMethods;
    private final TreeMaker treeMaker;

    public CleanupVisitor(Set<InvocableTree> allFixedMethods, Trees trees, Messager messager, TreeMaker treeMaker) {
        super(trees, messager);
        this.allFixedMethods = allFixedMethods;
        this.treeMaker = treeMaker;
    }

    /**
     * Fix types of variables declared with var that have an error type
     */
    @Override
    public Void visitVariable(VariableTree variableTree, Void ignored) {
        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR) {
            for (var fixedMethod : allFixedMethods) {
                var initializer = variableTree.getInitializer();
                if (initializer != null &&
                        initializer.getKind() == Tree.Kind.METHOD_INVOCATION
                        && fixedMethod.getWrapped() == initializer) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Error var initializer: " + variableTree.getInitializer());
                    var asJC = (JCTree.JCVariableDecl) variableTree;
                    asJC.vartype = treeMaker.Type(fixedMethod.getReturnType());
                    asJC.type = asJC.vartype.type;
                    messager.printMessage(Diagnostic.Kind.NOTE, "Assigned type : " + asJC.vartype);
                }
            }
        }
        return super.visitVariable(variableTree, ignored);
    }
}
