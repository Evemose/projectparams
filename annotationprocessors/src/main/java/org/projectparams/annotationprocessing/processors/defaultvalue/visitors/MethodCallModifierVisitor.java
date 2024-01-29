package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.invocabletree.MethodInvocableTree;
import org.projectparams.annotationprocessing.astcommons.invocabletree.NewClassInvocableTree;
import org.projectparams.annotationprocessing.astcommons.visitors.AbstractVisitor;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.ArgumentSupplier;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.Set;

public class MethodCallModifierVisitor extends AbstractVisitor<Void, InvocableInfo> {
    private final Set<InvocableTree> fixedMethodsInIteration;
    private final Set<InvocableTree> allFixedMethods;
    private final ArgumentSupplier argumentSupplier;

    public MethodCallModifierVisitor(Set<InvocableTree> fixedMethodsInIteration,
                                     Trees trees,
                                     ArgumentSupplier argumentSupplier,
                                     Messager messager, Set<InvocableTree> allFixedMethods) {
        super(trees, messager);
        this.fixedMethodsInIteration = fixedMethodsInIteration;
        this.allFixedMethods = allFixedMethods;
        this.argumentSupplier = argumentSupplier;
    }

    private void fixMethod(InvocableInfo invocableInfo,
                           InvocableTree call,
                           List<JCTree.JCExpression> args) {
        call.setArguments(args.toArray(new JCTree.JCExpression[0]));
        call.setReturnType(invocableInfo.returnTypeQualifiedName());
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, InvocableInfo invocableInfo) {
        visitInvocable(new MethodInvocableTree(invocation, getCurrentPath()), invocableInfo);
        return super.visitMethodInvocation(invocation, invocableInfo);
    }

    private void visitInvocable(InvocableTree invocation, InvocableInfo invocableInfo) {
        //messager.printMessage(Diagnostic.Kind.NOTE, "Visiting invocation: " + invocation);

        if (!allFixedMethods.contains(invocation) &&
                invocableInfo.matches(invocation)) {
            List<JCTree.JCExpression> args;
            try {
                args = argumentSupplier.getModifiedArguments(invocation, invocableInfo, getCurrentPath());
            } catch (UnsupportedSignatureException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                throw new RuntimeException(e);
            }
            fixMethod(invocableInfo, invocation, args);
            fixedMethodsInIteration.add(invocation);
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed invocation: " + invocation);
        }
    }

    @Override
    public Void visitNewClass(NewClassTree that, InvocableInfo invocableInfo) {
        visitInvocable(new NewClassInvocableTree(that, getCurrentPath()), invocableInfo);
        return super.visitNewClass(that, invocableInfo);
    }
}
