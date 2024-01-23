package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.visitors.CleanupVisitor;
import org.projectparams.annotationprocessing.processors.GlobalAnnotationProcessor;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.DefaultArgumentSupplier;
import org.projectparams.annotationprocessing.processors.defaultvalue.visitors.MethodCallModifier;
import org.projectparams.annotations.DefaultValue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class DefaultValueProcessor extends GlobalAnnotationProcessor<DefaultValue> {

    public DefaultValueProcessor(Trees trees, TreeMaker treeMaker, PackageElement rootPackage, Messager messager) {
        super(trees, treeMaker, rootPackage, messager);
    }

    @Override
    public void process(Set<Element> elements) {
        var methods = elements.stream().map(Element::getEnclosingElement)
                .collect(() -> Collections.newSetFromMap(new IdentityHashMap<ExecutableElement, Boolean>()),
                        (set, element) -> set.add((ExecutableElement) element),
                        Set::addAll);
        messager.printMessage(Diagnostic.Kind.NOTE, "Methods to process: " + methods);
        var argumentSupplier = new DefaultArgumentSupplier(treeMaker);
        var fixedMethodsInIteration = Collections.newSetFromMap(new IdentityHashMap<MethodInvocationTree, Boolean>(methods.size()));
        var allFixedMethods = Collections.newSetFromMap(new IdentityHashMap<MethodInvocationTree, Boolean>(methods.size()));
        do {
            allFixedMethods.addAll(fixedMethodsInIteration);
            fixedMethodsInIteration.clear();
            methods.forEach(method -> {
                var methodInfo = MethodInfo.from(method);
                messager.printMessage(Diagnostic.Kind.NOTE, "Method info: " + methodInfo);

                var fixedMethodsSize = fixedMethodsInIteration.size();
                // modify
                var modifier = new MethodCallModifier(fixedMethodsInIteration,
                        null,
                        trees,
                        argumentSupplier,
                        messager,
                        allFixedMethods);
                packageTree.accept(modifier, methodInfo);
                if (fixedMethodsInIteration.size() > fixedMethodsSize) {
                    messager.printMessage(Diagnostic.Kind.NOTE, "Fixed method: " + methodInfo);
                    messager.printMessage(Diagnostic.Kind.NOTE, "Fixing elements related to: " + fixedMethodsInIteration);
                    var cleanupVisitor = new CleanupVisitor(fixedMethodsInIteration, trees, messager, treeMaker);
                    packageTree.accept(cleanupVisitor, null);
                }
            });
        } while (!fixedMethodsInIteration.isEmpty());
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }

}
