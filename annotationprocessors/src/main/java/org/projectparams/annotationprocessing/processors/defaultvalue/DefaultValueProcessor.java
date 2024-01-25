package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.visitors.CleanupVisitor;
import org.projectparams.annotationprocessing.astcommons.visitors.LoggingVisitor;
import org.projectparams.annotationprocessing.astcommons.visitors.PrepareNewClassTreesVisitor;
import org.projectparams.annotationprocessing.processors.GlobalAnnotationProcessor;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.DefaultArgumentSupplier;
import org.projectparams.annotationprocessing.processors.defaultvalue.visitors.MethodCallModifierVisitor;
import org.projectparams.annotations.DefaultValue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultValueProcessor extends GlobalAnnotationProcessor<DefaultValue> {

    public DefaultValueProcessor(Trees trees, TreeMaker treeMaker, PackageElement rootPackage, Messager messager) {
        super(trees, treeMaker, rootPackage, messager);
    }

    @Override
    public void process(Set<Element> elements) {
        var methods = elements.stream().map(el -> (ExecutableElement) el.getEnclosingElement())
                .collect(Collectors.toUnmodifiableSet());
        messager.printMessage(Diagnostic.Kind.NOTE, "Methods to process: " + methods);
        var argumentSupplier = new DefaultArgumentSupplier(treeMaker);
        var fixedMethodsInIteration = new HashSet<InvocableTree>();
        var allFixedMethods = new HashSet<InvocableTree>();

        var prepareNewClassTreesVisitor = new PrepareNewClassTreesVisitor(trees, messager);
        packageTree.accept(prepareNewClassTreesVisitor, null);

        do {
            allFixedMethods.addAll(fixedMethodsInIteration);
            fixedMethodsInIteration.clear();
            methods.forEach(method -> {
                var methodInfo = InvocableInfo.from(method);
                //messager.printMessage(Diagnostic.Kind.NOTE, "Method info: " + methodInfo);

                // modify
                var modifier = new MethodCallModifierVisitor(fixedMethodsInIteration,
                        trees,
                        argumentSupplier,
                        messager,
                        allFixedMethods);
                packageTree.accept(modifier, methodInfo);


                var cleanupVisitor = new CleanupVisitor(allFixedMethods, trees, messager, treeMaker);
                packageTree.accept(cleanupVisitor, null);
            });
        } while (!fixedMethodsInIteration.isEmpty());

//        var loggingVisitor = new LoggingVisitor(trees, messager);
//        packageTree.accept(loggingVisitor, null);
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }

}
