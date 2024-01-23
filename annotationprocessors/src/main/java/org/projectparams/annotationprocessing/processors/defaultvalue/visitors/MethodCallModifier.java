package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.invocabletree.MethodInvocableTree;
import org.projectparams.annotationprocessing.astcommons.visitors.ParentDependentVisitor;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.ArgumentSupplier;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.Set;

public class MethodCallModifier extends ParentDependentVisitor<Void, MethodInfo, InvocableTree> {
    private final Set<InvocableTree> fixedMethodsInIteration;
    private final Set<InvocableTree> allFixedMethods;
    private final ArgumentSupplier argumentSupplier;

    public MethodCallModifier(Set<InvocableTree> fixedMethodsInIteration,
                              InvocableTree parent,
                              Trees trees,
                              ArgumentSupplier argumentSupplier,
                              Messager messager, Set<InvocableTree> allFixedMethods) {
        super(trees, messager,  parent);
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
        var needToPassLower = visitInvocable(new MethodInvocableTree(invocation, getCurrentPath()), methodInfo);
        if (needToPassLower) {
            return super.visitMethodInvocation(invocation, methodInfo);
        }
        return null;
    }

    private boolean visitInvocable(InvocableTree invocation, MethodInfo methodInfo) {
        messager.printMessage(Diagnostic.Kind.NOTE, "Processing method invocation: " + invocation);
        if (!allFixedMethods.contains(invocation) &&
                methodInfo.matches(invocation)) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Processing matched method: " + invocation);
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
            // if method invocation is fixed, it implies that all its arguments are fixed too,
            // otherwise it would be argument types mismatch with method signature
            // and method invocation would not be matched
            return false;
        } else if (!invocation.equals(parent)) {
            new MethodCallModifier(fixedMethodsInIteration, invocation, trees, argumentSupplier, messager, allFixedMethods)
                    .scan(new TreePath(getCurrentPath(), invocation), methodInfo);
            // read comment above to understand why we return false here
            return false;
        }
        return true;
    }

    // TODO: implement default values for constructors
    @Override
    public Void visitNewClass(NewClassTree that, MethodInfo methodInfo) {
//        var adapted = new NewClassToMethodInvocationTreeAdapter(that);
//        parent = adapted;
        return super.visitNewClass(that, methodInfo);
    }
}
