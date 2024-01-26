package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

@SuppressWarnings("unused")
public class LoggingVisitor extends AbstractVisitor<Void, Void> {
    public LoggingVisitor(Trees trees, Messager messager) {
        super(trees, messager);
    }

    @Override
    public Void visitMethodInvocation(com.sun.source.tree.MethodInvocationTree methodInvocationTree, Void aVoid) {
        var asJC = (JCTree.JCMethodInvocation) methodInvocationTree;
        messager.printMessage(Diagnostic.Kind.NOTE, "Method invocation: " + methodInvocationTree + " with type: " + asJC.meth.type);
        return super.visitMethodInvocation(methodInvocationTree, aVoid);
    }

    @Override
    public Void visitNewClass(com.sun.source.tree.NewClassTree newClassTree, Void aVoid) {
        var asJC = (JCTree.JCNewClass) newClassTree;
        messager.printMessage(Diagnostic.Kind.NOTE, "New class: " + newClassTree + " with type: " + asJC.constructorType);
        return super.visitNewClass(newClassTree, aVoid);
    }

    @Override
    public Void visitVariable(com.sun.source.tree.VariableTree variableTree, Void aVoid) {
        var asJC = (JCTree.JCVariableDecl) variableTree;
        messager.printMessage(Diagnostic.Kind.NOTE, "Variable: " + variableTree + " with type: " + variableTree.getType() + " and initializer: " +
                variableTree.getInitializer() + " and var type: " + asJC.vartype);
        return super.visitVariable(variableTree, aVoid);
    }
}
