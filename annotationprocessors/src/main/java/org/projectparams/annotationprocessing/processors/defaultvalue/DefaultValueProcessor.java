package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.visitors.CleanupVisitor;
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

                // modify
                var modifier = new MethodCallModifier(fixedMethodsInIteration, null, trees, treeMaker, messager);
                packageTree.accept(modifier, methodInfo);

                // run cleanup
                messager.printMessage(Diagnostic.Kind.NOTE, "Fixing elements related to: " + fixedMethodsInIteration);
                var cleanupVisitor = new CleanupVisitor(fixedMethodsInIteration, trees, messager, treeMaker);
                packageTree.accept(cleanupVisitor, null);


                packageTree.accept(cleanupVisitor, null);
            });
        } while (!fixedMethodsInIteration.isEmpty() && count < 5);
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }

}
