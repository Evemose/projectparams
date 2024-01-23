package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.invocabletree.MethodInvocableTree;
import org.projectparams.annotationprocessing.astcommons.visitors.AbstractVisitor;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.ArgumentSupplier;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.Set;

public class MethodCallModifier extends AbstractVisitor<Void, MethodInfo> {
    private final Set<InvocableTree> fixedMethodsInIteration;
    private final Set<InvocableTree> allFixedMethods;
    private final ArgumentSupplier argumentSupplier;

    public MethodCallModifier(Set<InvocableTree> fixedMethodsInIteration,
                              Trees trees,
                              ArgumentSupplier argumentSupplier,
                              Messager messager, Set<InvocableTree> allFixedMethods) {
        super(trees, messager);
        this.fixedMethodsInIteration = fixedMethodsInIteration;
        this.allFixedMethods = allFixedMethods;
        this.argumentSupplier = argumentSupplier;
    }

    private void fixMethod(MethodInfo methodInfo,
                           InvocableTree call,
                           List<JCTree.JCExpression> args) {
        call.setArguments(args.toArray(new JCTree.JCExpression[0]));
        call.setReturnType(methodInfo.returnTypeQualifiedName());
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, MethodInfo methodInfo) {
        visitInvocable(new MethodInvocableTree(invocation, getCurrentPath()), methodInfo);
        return super.visitMethodInvocation(invocation, methodInfo);
    }

    private void visitInvocable(InvocableTree invocation, MethodInfo methodInfo) {
        messager.printMessage(Diagnostic.Kind.NOTE, "Inspecting method invocation: " + invocation);
        if (!allFixedMethods.contains(invocation) &&
                methodInfo.matches(invocation)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixing matched method invocation: " + invocation);
            List<JCTree.JCExpression> args;
            try {
                args = argumentSupplier.getModifiedArguments(invocation, methodInfo);
            } catch (UnsupportedSignatureException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                throw new RuntimeException(e);
            }
            fixMethod(methodInfo, invocation, args);
            fixedMethodsInIteration.add(invocation);
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed method invocation: " + invocation);
        }
    }

    // TODO: implement default values for constructors
    @Override
    public Void visitNewClass(NewClassTree that, MethodInfo methodInfo) {
//        var adapted = new NewClassToMethodInvocationTreeAdapter(that);
//        parent = adapted;
        return super.visitNewClass(that, methodInfo);
    }
}
