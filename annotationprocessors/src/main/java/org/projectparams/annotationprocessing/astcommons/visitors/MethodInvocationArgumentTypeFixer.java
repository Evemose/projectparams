package org.projectparams.annotationprocessing.astcommons.visitors;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.Messager;
import java.util.Set;

public class MethodInvocationArgumentTypeFixer extends AbstractVisitor<Void, Void> {
    private final Set<MethodInvocationTree> fixedMethodsInIteration;
    private final MethodInvocationTree parentMethodInvocation;

    public MethodInvocationArgumentTypeFixer(Set<MethodInvocationTree> fixedMethodsInIteration,
                                             MethodInvocationTree parentMethodInvocation,
                                             Trees trees,
                                             Messager messager) {
        super(trees, messager);
        this.fixedMethodsInIteration = fixedMethodsInIteration;
        this.parentMethodInvocation = parentMethodInvocation;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree invocation, Void ignored) {
//        messager.printMessage(Diagnostic.Kind.NOTE, "Visiting method invocation: " + invocation + " for parent: "
//                + parentMethodInvocation + " has been fixed: " + fixedMethodsInIteration.contains(invocation));
        if (fixedMethodsInIteration.contains(invocation)) {
            fixParentSignature(invocation);
        } else {
            passFixLower(invocation);
        }
        return null;
    }

    private void passFixLower(MethodInvocationTree invocation) {
        passFixToArgs(invocation);
        passFixToTarget(invocation);
    }

    private void passFixToTarget(MethodInvocationTree invocation) {
        if (invocation.getMethodSelect() instanceof MemberSelectTree) {
            new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, invocation, trees, messager)
                    .scan(new TreePath(getCurrentPath(), invocation.getMethodSelect()), null);
        }
    }

    private void passFixToArgs(MethodInvocationTree invocation) {
        invocation.getArguments().forEach(arg -> {
            var argPath = new TreePath(getCurrentPath(), arg);
//            messager.printMessage(Diagnostic.Kind.NOTE, "Visiting method invocation arg: " + arg
//                    + " with type: " + ((JCTree.JCExpression) arg).type);
            new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, invocation, trees, messager)
                    .scan(argPath, null);
        });
    }

    private void fixParentSignature(MethodInvocationTree invocation) {
        var parentAsJC = (JCTree.JCMethodInvocation) parentMethodInvocation;
        var parentMethodSelect = parentMethodInvocation.getMethodSelect();
        if (parentMethodSelect instanceof MemberSelectTree memberSelectTree
                && memberSelectTree.getExpression() == invocation) {
            fixParentTarget((JCTree.JCMethodInvocation) invocation, parentAsJC);
        } else {
            fixParentCorrespondingArg(invocation, parentAsJC);
        }
    }

    private void fixParentCorrespondingArg(MethodInvocationTree invocation, JCTree.JCMethodInvocation parentAsJC) {
        //messager.printMessage(Diagnostic.Kind.NOTE, "Trying to fix method invocation (cleanup): " + parentMethodInvocation);
        parentAsJC.args.stream().filter(arg -> arg.equals(invocation))
                .findFirst().ifPresent(arg -> {
                    arg.type = ((JCTree.JCMethodInvocation) invocation).meth.type.getReturnType();
                    //messager.printMessage(Diagnostic.Kind.NOTE, "Corrected arg type: " + arg.type);
                });
    }

    private void fixParentTarget(JCTree.JCMethodInvocation invocation, JCTree.JCMethodInvocation parentAsJC) {
        //messager.printMessage(Diagnostic.Kind.NOTE, "Trying to fix method invocation target (cleanup): " + parentMethodInvocation);
        parentAsJC.meth.type.tsym = invocation.meth.type.getReturnType().asElement();
//        messager.printMessage(Diagnostic.Kind.NOTE, "Corrected target type: " +
//                parentAsJC.meth.type);
    }
}
