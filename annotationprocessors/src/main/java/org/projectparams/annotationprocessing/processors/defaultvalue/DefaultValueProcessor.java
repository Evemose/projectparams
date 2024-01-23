package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.processors.GlobalAnnotationProcessor;
import org.projectparams.annotationprocessing.processors.defaultvalue.visitors.MethodCallModifier;
import org.projectparams.annotations.DefaultValue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultValueProcessor extends GlobalAnnotationProcessor<DefaultValue> {

    public DefaultValueProcessor(Trees trees, TreeMaker treeMaker, PackageElement rootPackage, Messager messager) {
        super(trees, treeMaker, rootPackage, messager);
    }

    @Override
    public void process(Set<Element> elements) throws Exception {
        var methods = elements.stream().map(Element::getEnclosingElement)
                .collect(() -> Collections.newSetFromMap(new IdentityHashMap<ExecutableElement, Boolean>()),
                        (set, element) -> set.add((ExecutableElement) element),
                        Set::addAll);
        messager.printMessage(Diagnostic.Kind.NOTE, "Methods to process: " + methods);
        var fixedMethodsInIteration = Collections.newSetFromMap(new IdentityHashMap<MethodInvocationTree, Boolean>(methods.size()));
        var count = 0;
        do {
            // TODO: dont forget to remove when done
            count++;
            fixedMethodsInIteration.clear();
            methods.forEach(method -> {
                var methodInfo = MethodInfo.from(method);
                messager.printMessage(Diagnostic.Kind.NOTE, "Method info: " + methodInfo);
                //messager.printMessage(Diagnostic.Kind.NOTE, "Processing method: " + methodInfo);

                // wrapper needed to path all method declarations in class and superclasses
                // necessary to check implicit this.method() calls
                var methodModifierWrapper = new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitClass(ClassTree classTree, Void ignored) {
                        var element = (TypeElement) trees.getElement(getCurrentPath());
                        var declaredMethods = new HashSet<MethodInfo>();
                        do {
                            // TODO: filter private methods in superclasses
                            declaredMethods.addAll(element.getEnclosedElements().stream()
                                    .filter(enclosedElement -> enclosedElement instanceof ExecutableElement)
                                    .map(enclosedElement -> {
                                        var methInfo = MethodInfo.from((ExecutableElement) enclosedElement);
                                        declaredMethods.removeIf(meth ->
                                                meth.withNullOwnerAndDefault().equals(methInfo.withNullOwnerAndDefault()));
                                        return methInfo;
                                    })
                                    .collect(Collectors.toUnmodifiableSet()));
                            var superClass = element.getSuperclass();
                            if (superClass != null && superClass.getKind() == TypeKind.DECLARED) {
                                element = (TypeElement) ((DeclaredType) superClass).asElement();
                            } else {
                                element = null;
                            }
                        } while (element != null);
                        new MethodCallModifier(fixedMethodsInIteration, classTree, trees, treeMaker, messager)
                                .scan(new TreePath(getCurrentPath(), classTree), methodInfo);
                        return super.visitClass(classTree, ignored);
                    }
                };
                packageTree.accept(methodModifierWrapper, null);

                // needed to fix variable declarations that use type inference
                messager.printMessage(Diagnostic.Kind.NOTE, "Fixing elements related to: " + fixedMethodsInIteration);
                var variablesTypeFixer = new TreePathScanner<Void, MethodInfo>() {
                    @Override
                    public Void visitVariable(VariableTree variableTree, MethodInfo methodInfo) {
                        if (TypeUtils.getTypeKind(getCurrentPath()) == TypeKind.ERROR) {
                            var initializer = variableTree.getInitializer();
                            if (initializer != null) {
                                messager.printMessage(Diagnostic.Kind.NOTE, "Error var initializer: " + variableTree.getInitializer());
                                var asJC = (JCTree.JCVariableDecl) variableTree;
                                asJC.vartype = treeMaker.Type(((JCTree.JCMethodInvocation) initializer).meth.type.getReturnType());
                                messager.printMessage(Diagnostic.Kind.NOTE, "Assigned type : " + asJC.vartype);
                            }
                        }
                        return super.visitVariable(variableTree, methodInfo);
                    }

                    @Override
                    public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
                        new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, that)
                                .scan(new TreePath(getCurrentPath(), that), null);
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

        public MethodInvocationArgumentTypeFixer(Set<MethodInvocationTree> fixedMethodsInIteration,
                                                 MethodInvocationTree parentMethodInvocation) {
            this.fixedMethodsInIteration = fixedMethodsInIteration;
            this.parentMethodInvocation = parentMethodInvocation;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree invocation, Void ignored) {
            messager.printMessage(Diagnostic.Kind.NOTE, "Visiting method invocation: " + invocation + " for parent: "
                    + parentMethodInvocation + " has been fixed: " + fixedMethodsInIteration.contains(invocation));
            if (fixedMethodsInIteration.contains(invocation)) {
                var parentAsJC = (JCTree.JCMethodInvocation) parentMethodInvocation;
                var parentMethodSelect = parentMethodInvocation.getMethodSelect();
                if (parentMethodSelect instanceof MemberSelectTree memberSelectTree
                        && memberSelectTree.getExpression() == invocation) {
                    // alter target
                    messager.printMessage(Diagnostic.Kind.NOTE, "Trying to fix method invocation target (cleanup): " + parentMethodInvocation);
                    parentAsJC.meth.type.tsym = ((JCTree.JCMethodInvocation) invocation).meth.type.getReturnType().asElement();
                    messager.printMessage(Diagnostic.Kind.NOTE, "Corrected target type: " +
                            parentAsJC.meth.type);
                } else {
                    // otherwise alter arg
                    messager.printMessage(Diagnostic.Kind.NOTE, "Trying to fix method invocation (cleanup): " + parentMethodInvocation);
                    parentAsJC.args.stream().filter(arg -> arg.equals(invocation))
                            .findFirst().ifPresent(arg -> {
                                arg.type = ((JCTree.JCMethodInvocation) invocation).meth.type.getReturnType();
                                messager.printMessage(Diagnostic.Kind.NOTE, "Corrected arg type: " + arg.type);
                            });
                }
            } else {
                // visit args
                invocation.getArguments().forEach(arg -> {
                    var argPath = new TreePath(getCurrentPath(), arg);
                    messager.printMessage(Diagnostic.Kind.NOTE, "Visiting method invocation arg: " + arg
                            + " with type: " + ((JCTree.JCExpression) arg).type);
                        new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, invocation)
                                .scan(argPath, null);
                    // visit target
                });
                if (invocation.getMethodSelect() instanceof MemberSelectTree) {
                    new MethodInvocationArgumentTypeFixer(fixedMethodsInIteration, invocation)
                            .scan(new TreePath(getCurrentPath(), invocation.getMethodSelect()), null);
                }
            }
            return null;
        }
    }

}
