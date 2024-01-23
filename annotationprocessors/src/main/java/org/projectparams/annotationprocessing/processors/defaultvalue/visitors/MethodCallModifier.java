package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.visitors.ParentDependentVisitor;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.ArgumentSupplier;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.Set;

public class MethodCallModifier extends ParentDependentVisitor<Void, MethodInfo, MethodInvocationTree> {
    private final Set<MethodInvocationTree> fixedMethodsInIteration;
    private final Set<MethodInvocationTree> allFixedMethods;
    private final ArgumentSupplier argumentSupplier;

    public MethodCallModifier(Set<MethodInvocationTree> fixedMethodsInIteration,
                              MethodInvocationTree parent,
                              Trees trees,
                              ArgumentSupplier argumentSupplier,
                              Messager messager, Set<MethodInvocationTree> allFixedMethods) {
        super(trees, messager,  parent);
        this.fixedMethodsInIteration = fixedMethodsInIteration;
        this.allFixedMethods = allFixedMethods;
        this.argumentSupplier = argumentSupplier;
        this.parent = parent;
    }
    private void modifyMethodArgs(MethodInfo methodInfo,
                                         JCTree.JCMethodInvocation call,
                                         List<JCTree.JCExpression> args) {
        call.args = args;
        call.meth.type = new Type.MethodType(
                List.from(args.stream().map(arg -> arg.type).toList()),
                TypeUtils.getTypeByName(methodInfo.returnTypeQualifiedName()),
                List.nil(),
                TypeUtils.getTypeByName(methodInfo.ownerQualifiedName()).asElement());
        call.type = call.meth.type.getReturnType();
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
        messager.printMessage(Diagnostic.Kind.NOTE, "Processing method invocation: " + that);
        if (!allFixedMethods.contains(that) &&
                methodInfo.matches(that, trees, getCurrentPath())) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Processing matched method: " + that);
            var call = (JCTree.JCMethodInvocation) that;
            List<JCTree.JCExpression> args;
            try {
                args = argumentSupplier.getModifiedArguments(call, methodInfo);
            } catch (UnsupportedSignatureException e) {
                messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                throw new RuntimeException(e);
            }
            modifyMethodArgs(methodInfo, call, args);
            fixedMethodsInIteration.add(that);
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed method invocation: " + that);
            // if method invocation is fixed, it implies that all its arguments are fixed too,
            // otherwise it would be argument types mismatch with method signature
            // and method invocation would not be matched
            return null;
        } else if (that != parent) {
            new MethodCallModifier(fixedMethodsInIteration, that, trees, argumentSupplier, messager, allFixedMethods)
                    .scan(new TreePath(getCurrentPath(), that), methodInfo);
            // read comment above to understand why we return null here
            return null;
        }
        return super.visitMethodInvocation(that, methodInfo);
    }

    // TODO: implement default values for constructors
    @Override
    public Void visitNewClass(NewClassTree that, MethodInfo methodInfo) {
//        var adapted = new NewClassToMethodInvocationTreeAdapter(that);
//        parent = adapted;
        return super.visitNewClass(that, methodInfo);
    }
}
