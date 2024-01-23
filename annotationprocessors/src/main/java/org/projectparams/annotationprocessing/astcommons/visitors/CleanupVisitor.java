package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;

import javax.annotation.processing.Messager;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.Set;

public class CleanupVisitor extends AbstractVisitor<Void, Void> {
    private final Set<MethodInvocationTree> fixedMethodsInIteration;

    public CleanupVisitor(Set<MethodInvocationTree> fixedMethodsInIteration, Trees trees, Messager messager, TreeMaker treeMaker) {
        super(trees, messager, treeMaker);
        this.fixedMethodsInIteration = fixedMethodsInIteration;
    }

    /**
     * Fix types of variables declared with var that have an error type
     */
    @Override
    public Void visitVariable(VariableTree variableTree, Void ignored) {
        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR) {
            var initializer = variableTree.getInitializer();
            if (initializer != null) {
                messager.printMessage(Diagnostic.Kind.NOTE, "Error var initializer: " + variableTree.getInitializer());
                var asJC = (JCTree.JCVariableDecl) variableTree;
                asJC.vartype = treeMaker.Type(((JCTree.JCMethodInvocation) initializer).meth.type.getReturnType());
                messager.printMessage(Diagnostic.Kind.NOTE, "Assigned type : " + asJC.vartype);
            }
        }
        return super.visitVariable(variableTree, ignored);
    }

    /**
     * Fix types of method invocations that either invoked by return value
     * of method fixed in this iteration or have a fixed method result as an argument.
     * Delegates fix to {@link MethodInvocationArgumentTypeFixer}.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree that, Void ignored) {
        new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, that, trees, treeMaker, messager)
                .scan(new TreePath(getCurrentPath(), that), null);
        return null;
    }
}
