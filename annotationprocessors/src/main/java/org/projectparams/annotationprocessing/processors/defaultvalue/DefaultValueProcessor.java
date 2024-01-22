package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.ast.TypeUtils;
import org.projectparams.annotationprocessing.processors.GlobalAnnotationProcessor;
import org.projectparams.annotations.DefaultValue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultValueProcessor extends GlobalAnnotationProcessor<DefaultValue> {

    public DefaultValueProcessor(Trees trees, TreeMaker treeMaker, PackageElement rootPackage, Messager messager) {
        super(trees, treeMaker, rootPackage, messager);
    }

    @Override
    public void process(Set<Element> elements) throws Exception {
        var methods = elements.stream().map(Element::getEnclosingElement).collect(Collectors.toUnmodifiableSet());
        var fixedMethodsInIteration = Collections.newSetFromMap(new IdentityHashMap<MethodInvocationTree, Boolean>(methods.size()));
        var count = 0;
        do {
            count++;
            fixedMethodsInIteration.clear();
            methods.forEach(method -> {
                var methodInfo = MethodInfo.from((ExecutableElement) method);
                messager.printMessage(Diagnostic.Kind.NOTE, "Processing method: " + methodInfo);

                var methodModifier = new TreePathScanner<Void, MethodInfo>() {
                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
                        messager.printMessage(Diagnostic.Kind.NOTE, "Processing method invocation: " + that);
                        if (methodInfo.matches(that)) {
                            messager.printMessage(Diagnostic.Kind.NOTE, "Processing matched method: " + that);
                            messager.printMessage(Diagnostic.Kind.NOTE, "Type tag: " + TypeUtils.getTypeByName(methodInfo.parameterTypeQualifiedNames()[0]).getTag());
                            messager.printMessage(Diagnostic.Kind.NOTE, "Default value: " + methodInfo.paramIndexToDefaultValue().get(0));
                            var literal = treeMaker.Literal(
                                    TypeUtils.getTypeByName(methodInfo.parameterTypeQualifiedNames()[0]).getTag(),
                                    methodInfo.paramIndexToDefaultValue().get(0));
                            var call = (JCTree.JCMethodInvocation) that;
                            if (call.args.isEmpty()) {
                                call.args = call.args.append(literal);
                                call.meth.type = new Type.MethodType(
                                        List.from(Arrays.stream(methodInfo
                                                        .parameterTypeQualifiedNames())
                                                .map(TypeUtils::getTypeByName).toList()),
                                        TypeUtils.getTypeByName(methodInfo.returnTypeQualifiedName()),
                                        List.nil(),
                                        TypeUtils.getTypeByName(methodInfo.ownerQualifiedName()).asElement());
                            }
                            fixedMethodsInIteration.add(that);
                            messager.printMessage(Diagnostic.Kind.NOTE, "Fixed method invocation: " + that);
                            // if method invocation is fixed, it implies that all its arguments are fixed too,
                            // otherwise it would be argument types mismatch with method signature
                            // and method invocation would not be matched
                            return null;
                        }
                        return super.visitMethodInvocation(that, methodInfo);
                    }
                };
                packageTree.accept(methodModifier, methodInfo);

                // needed to fix variable declarations that use type inference
                var variablesTypeFixer = new TreePathScanner<Void, MethodInfo>() {
                    @Override
                    public Void visitVariable(VariableTree variableTree, MethodInfo methodInfo) {
                        var element = trees.getElement(getCurrentPath());
                        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR) {
                            messager.printMessage(Diagnostic.Kind.NOTE, "Error var initializer: " + variableTree.getInitializer());
                            var initializer = variableTree.getInitializer();
                            // 0 - type, 1 - marker that highest level method call was already visited
                            var returnTypeContainer = new TypeMirror[1];
                            var typeObtainer = new TreePathScanner<Void, TypeMirror[]>() {
                                @Override
                                public Void visitMethodInvocation(MethodInvocationTree that, TypeMirror[] exprType) {
                                    messager.printMessage(Diagnostic.Kind.NOTE, "Does contain: " + fixedMethodsInIteration.contains(that) + " " + that);
                                    if (fixedMethodsInIteration.contains(that)
                                            && methodInfo.matches(that)) {
                                        exprType[0] = TypeUtils.getTypeByName(methodInfo.returnTypeQualifiedName());
                                    }
                                    return null;
                                }
                            };
                            typeObtainer.scan(new TreePath(getCurrentPath(), initializer), returnTypeContainer);
                            messager.printMessage(Diagnostic.Kind.NOTE, "Assigned type : " + returnTypeContainer[0]);
                            if (returnTypeContainer[0] != null) {
                                var newType = returnTypeContainer[0];
                                var asDecl = (JCTree.JCVariableDecl) variableTree;
                                asDecl.vartype = treeMaker.Type((Type) newType);
                            }
                        }
                        return super.visitVariable(variableTree, methodInfo);
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
                        var element = trees.getElement(getCurrentPath());
                        that.getArguments().forEach(arg ->
                                new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, that)
                                        .scan(new TreePath(getCurrentPath(), arg), null));
                        return null;
                    }
                };
                packageTree.accept(variablesTypeFixer, methodInfo);
            });
        } while (!fixedMethodsInIteration.isEmpty() && count < 5);
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }

    private class MethodInvocationArgumentTypeFixer extends TreePathScanner<Void, Void> {
        private final Set<MethodInvocationTree> fixedMethodsInIteration;
        private final MethodInvocationTree parentMethodInvocation;

        public MethodInvocationArgumentTypeFixer(Set<MethodInvocationTree> fixedMethodsInIteration, MethodInvocationTree parentMethodInvocation) {
            this.fixedMethodsInIteration = fixedMethodsInIteration;
            this.parentMethodInvocation = parentMethodInvocation;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree invocation, Void ignored) {
            if (fixedMethodsInIteration.contains(invocation)) {
                var parentAsJC = (JCTree.JCMethodInvocation) parentMethodInvocation;
                var argToCorrect = parentAsJC.args.stream().filter(arg -> arg.equals(invocation))
                        .findFirst().get();
                argToCorrect.type = ((JCTree.JCMethodInvocation) invocation).meth.type.getReturnType();
                messager.printMessage(Diagnostic.Kind.NOTE, "Corrected type: " + argToCorrect.type);
            } else if (invocation != parentMethodInvocation) {
                invocation.getArguments().forEach(arg ->
                        new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, invocation)
                                .scan(new TreePath(getCurrentPath(), arg), null));
            }
            return null;
        }
    }
}
