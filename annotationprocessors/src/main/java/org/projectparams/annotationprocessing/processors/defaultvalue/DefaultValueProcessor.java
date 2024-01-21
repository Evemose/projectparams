package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultValueProcessor extends GlobalAnnotationProcessor<DefaultValue> {

    public DefaultValueProcessor(Trees trees, TreeMaker treeMaker, PackageElement rootPackage, Messager messager) {
        super(trees, treeMaker, rootPackage, messager);
    }

    @Override
    public void process(Set<Element> elements) throws Exception {
        var methods = elements.stream().map(Element::getEnclosingElement).collect(Collectors.toUnmodifiableSet());
        methods.forEach(method -> {
            var methodInfo = MethodInfo.from((ExecutableElement) method);
            messager.printMessage(Diagnostic.Kind.NOTE, "Processing method: " + methodInfo);

            var methodModifier = new TreePathScanner<Void, MethodInfo>() {
                @Override
                public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
                    var element = trees.getElement(getCurrentPath());
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing method invocation: " + element);
                    if (methodInfo.matches(that, trees, getCurrentPath())) {
                        messager.printMessage(Diagnostic.Kind.NOTE, "Method invocation matches: " + element);
                        messager.printMessage(Diagnostic.Kind.NOTE, "Method: " + methodInfo);
                        var literal = treeMaker.Literal(
                                TypeUtils.getTypeByName(methodInfo.parameterTypeQualifiedNames()[0]).getTag(),
                                methodInfo.paramIndexToDefaultValue().get(0));
                        messager.printMessage(Diagnostic.Kind.NOTE, "Literal: " + literal.getValue().getClass());
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
                        } else {
                            messager.printMessage(Diagnostic.Kind.NOTE, "Not modifying type: " + call.meth.type);
                        }
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
                        var returnTypeContainer = new TypeMirror[2];
                        var typeObtainer = new TreePathScanner<Void, TypeMirror[]>() {
                            @Override
                            public Void visitMethodInvocation(MethodInvocationTree that, TypeMirror[] exprType) {
                                if (exprType[1] == null && methodInfo.matches(that, trees, getCurrentPath())) {
                                    exprType[0] = TypeUtils.getTypeByName(methodInfo.returnTypeQualifiedName());
                                }
                                messager.printMessage(Diagnostic.Kind.NOTE,
                                        String.valueOf(methodInfo.matches(that, trees, getCurrentPath())));
                                exprType[1] = TypeUtils.getTypeByName("java.lang.Object");
                                return super.visitMethodInvocation(that, exprType);
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
            };
            packageTree.accept(variablesTypeFixer, methodInfo);
        });
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }
}
