package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.ast.TypeUtils;
import org.projectparams.annotationprocessing.processors.GlobalAnnotationProcessor;
import org.projectparams.annotations.DefaultValue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Set;
import java.util.stream.Collectors;
import com.sun.tools.javac.util.List;

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

            var anon = new TreePathScanner<Void, MethodInfo>() {
                @Override
                public Void visitMethodInvocation(MethodInvocationTree that, MethodInfo methodInfo) {
                    var element = trees.getElement(getCurrentPath());
                    messager.printMessage(Diagnostic.Kind.NOTE, "Processing method invocation: " + element);
                    if (methodInfo.matches(that, trees, getCurrentPath())) {
                        messager.printMessage(Diagnostic.Kind.NOTE, "Method invocation matches: " + element);
                        var literal = treeMaker.Literal("default");
                        var call = (JCTree.JCMethodInvocation) that;
                        call.args = call.args.append(literal);

                        // TODO: make this vor with var
                        var type = new Type.MethodType(List.of(TypeUtils.getTypeByName("java.lang.String")),
                                TypeUtils.getTypeByName("java.lang.String"),
                                List.nil(),
                                TypeUtils.getTypeByName("org.projectparams.test.Abobus").asElement());
                        call.meth.setType(type);
                    }
                    return super.visitMethodInvocation(that, methodInfo);
                }
            };
            packageTree.accept(anon, methodInfo);
        });
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }
}
