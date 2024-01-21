package org.projectparams.annotationprocessing.processors;

import javax.lang.model.element.Element;

import static com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

import java.lang.annotation.Annotation;
import java.util.Set;

public interface AnnotationProcessor<T extends Annotation> {
    void process(Set<Element> elements) throws Exception;
    Class<T> getProcessedAnnotation();
}