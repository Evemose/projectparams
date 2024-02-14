package org.projectparams.annotationprocessing.processors.defaultvalue;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.visitors.*;
import org.projectparams.annotationprocessing.processors.GlobalAnnotationProcessor;
import org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers.DefaultArgumentSupplier;
import org.projectparams.annotationprocessing.processors.defaultvalue.visitors.MemberRefsToLambdasVisitor;
import org.projectparams.annotationprocessing.processors.defaultvalue.visitors.MethodCallModifierVisitor;
import org.projectparams.annotations.DefaultValue;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
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
        var argumentSupplier = new DefaultArgumentSupplier();
        var fixedMethodsInIteration = new HashSet<InvocableTree>();
        var allFixedMethods = new HashSet<InvocableTree>();

        var invocablePool =
                InvocableInfoPool.of(
                        methods.stream().flatMap(method ->
                                InvocableInfo.from(method).stream()).toArray(InvocableInfo[]::new));

        invocablePool.forEach(methodInfo -> {
            if (!methodInfo.name().matches("(this)|(super)")) {
                new DefaultValueInjector(methodInfo).inject();
            }
        });

        var memberRefsToLambdasVisitor = new MemberRefsToLambdasVisitor(trees, messager);
        packageTree.accept(memberRefsToLambdasVisitor, null);

        var reevaluateTreePositionsVisitor = new ReevaluateTreePositionsVisitor();
        packageTree.accept(reevaluateTreePositionsVisitor, null);

        messager.printMessage(Diagnostic.Kind.NOTE, "Invocable pool: " + invocablePool + "\n\n\nStarting");

        do {
            allFixedMethods.addAll(fixedMethodsInIteration);
            fixedMethodsInIteration.clear();
            invocablePool.forEach(methodInfo -> {
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

        var postModificationAttributionVisitor = new PostModificationAttributionVisitor(treeMaker, trees, messager);
        packageTree.accept(postModificationAttributionVisitor, null);
    }

    @Override
    public Class<DefaultValue> getProcessedAnnotation() {
        return DefaultValue.class;
    }

}
