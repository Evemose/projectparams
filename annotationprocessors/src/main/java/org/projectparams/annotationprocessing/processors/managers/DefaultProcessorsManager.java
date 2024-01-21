package org.projectparams.annotationprocessing.processors.managers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.projectparams.annotationprocessing.processors.AnnotationProcessor;
import org.projectparams.annotationprocessing.processors.defaultvalue.DefaultValueProcessor;

import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DefaultProcessorsManager implements ProcessorsManager {
    /**
     * Set of already processed compilation units.
     * Used to prevent processing of the same compilation unit multiple times.
     * IdentityHashSet is used because JCCompilationUnits are mutated during processing.
     */
    private final Set<Element> processedUnits = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<AnnotationProcessor<?>> processors;
    private final Trees trees;

    public DefaultProcessorsManager(Trees trees, TreeMaker treeMaker, Names names, PackageElement rootPackage, Messager messager) {
        this.trees = trees;
        this.processors = Set.of(
                new DefaultValueProcessor(trees, treeMaker, rootPackage, messager)
        );
    }

    @Override
    public void process(RoundEnvironment roundEnv) throws Exception {
        for (var processor : processors) {
            processor.process(roundEnv.getElementsAnnotatedWith(processor.getProcessedAnnotation()).stream()
                    .filter(Predicate.not(processedUnits::contains)).collect(Collectors.toSet()));
        }

        processedUnits.addAll(roundEnv.getRootElements());
    }


}
