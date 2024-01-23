package org.projectparams.annotationprocessing.processors.defaultvalue.visitors;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.visitors.ParentDependentVisitor;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.Set;

public class MethodCallModifier extends ParentDependentVisitor<Void, MethodInfo, MethodInvocationTree> {
    private final Set<MethodInvocationTree> fixedMethodsInIteration;
    private final Set<MethodInvocationTree> allFixedMethods;

    public MethodCallModifier(Set<MethodInvocationTree> fixedMethodsInIteration,
                              MethodInvocationTree parent,
                              Trees trees,
                              TreeMaker treeMaker,
                              Messager messager, Set<MethodInvocationTree> allFixedMethods) {
        super(trees, messager, treeMaker, parent);
        this.fixedMethodsInIteration = fixedMethodsInIteration;
        this.allFixedMethods = allFixedMethods;
        this.parent = parent;
    }

    private static void updateOwner(MethodInfo methodInfo, JCTree.JCMethodInvocation call) {
        call.meth.type = new Type.MethodType(
                List.from(call.args.stream().map(arg -> arg.type).toList()),
                TypeUtils.getTypeByName(methodInfo.returnTypeQualifiedName()),
                List.nil(),
                TypeUtils.getTypeByName(methodInfo.ownerQualifiedName()).asElement());
    }

    private static void modifyMethodArgs(MethodInfo methodInfo, JCTree.JCMethodInvocation call, JCTree.JCLiteral literal) {
        call.args = call.args.append(literal);
        call.meth.type = new Type.MethodType(
                List.from(Arrays.stream(methodInfo
                                .parameterTypeQualifiedNames())
                        .map(TypeUtils::getTypeByName).toList()),
                TypeUtils.getTypeByName(methodInfo.returnTypeQualifiedName()),
                List.nil(),
                TypeUtils.getTypeByName(methodInfo.ownerQualifiedName()).asElement());
    }

    private static TypeTag getTypeTagOfParam(MethodInfo methodInfo) {
        TypeTag tag;
        if (methodInfo.paramIndexToDefaultValue().get(0).equals(MethodInfo.NULL)) {
            tag = TypeTag.BOT;
        } else {
            tag = TypeUtils.getTypeByName(methodInfo.parameterTypeQualifiedNames()[0]).getTag();
        }
        return tag;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
        if (!allFixedMethods.contains(that) &&
                methodInfo.matches(that, trees, getCurrentPath())) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Processing matched method: " + that);
            var call = (JCTree.JCMethodInvocation) that;
            if (call.args.isEmpty()) {
                var literal = getParamValue(methodInfo);
                messager.printMessage(Diagnostic.Kind.NOTE, "Type tag: " + TypeUtils.getTypeByName(methodInfo.parameterTypeQualifiedNames()[0]).getTag());
                messager.printMessage(Diagnostic.Kind.NOTE, "Default value: " + methodInfo.paramIndexToDefaultValue().get(0));
                modifyMethodArgs(methodInfo, call, literal);
            } else {
                messager.printMessage(Diagnostic.Kind.NOTE, "Args match, prev method return type: " + call.type);
                updateOwner(methodInfo, call);
                messager.printMessage(Diagnostic.Kind.NOTE, "Args match, new method return type: " + call.meth.type.getReturnType());
            }
            fixedMethodsInIteration.add(that);
            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed method invocation: " + that);
            // if method invocation is fixed, it implies that all its arguments are fixed too,
            // otherwise it would be argument types mismatch with method signature
            // and method invocation would not be matched
            return null;
        } else if (that != parent) {
            new MethodCallModifier(fixedMethodsInIteration, that, trees, treeMaker, messager, allFixedMethods)
                    .scan(new TreePath(getCurrentPath(), that), methodInfo);
            // read comment above to understand why we return null here
            return null;
        }
        return super.visitMethodInvocation(that, methodInfo);
    }

    // TODO: implement default values for constructors
    @Override
    public Void visitNewClass(NewClassTree that, MethodInfo methodInfo) {

        return super.visitNewClass(that, methodInfo);
    }

    private JCTree.JCLiteral getParamValue(MethodInfo methodInfo) {
        TypeTag tag = getTypeTagOfParam(methodInfo);
        return treeMaker.Literal(
                tag,
                methodInfo.paramIndexToDefaultValue().get(0));
    }
}
