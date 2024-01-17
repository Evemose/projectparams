package org.projectparams.processors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import org.projectparams.processors.ast.utils.ElementUtils;
import org.projectparams.processors.exceptions.ProcessingEnvironmentException;
import org.projectparams.processors.utils.ProcessingUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.sun.tools.javac.tree.JCTree.*;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
class MainProcessor extends AbstractProcessor {
    private JavacProcessingEnvironment javacProcessingEnv;
    private ProcessingEnvironment processingEnv;

    // TODO: remove related code if not needed in future
    private JavacFiler javacFiler;
    private Trees trees;
    private final Set<JCCompilationUnit> processedUnits = new HashSet<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            this.javacProcessingEnv = ProcessingUtils.getJavacProcessingEnvironment(processingEnv);
        } catch (ProcessingEnvironmentException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        this.processingEnv = processingEnv;
        try {
            this.javacFiler = ProcessingUtils.getJavacFiler(processingEnv.getFiler());
        } catch (ProcessingEnvironmentException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        this.trees = Trees.instance(javacProcessingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;

        var compilationUnits = roundEnv.getRootElements().stream()
                .mapMulti((Element element, Consumer<JCCompilationUnit> consumer) ->
                        ElementUtils.getCompilationUnit(trees, element).ifPresent(consumer))
                .filter(Predicate.not(processedUnits::contains))
                .toList();

        if (compilationUnits.isEmpty()) return false;

        // TODO: add processing logic here
        // compilationUnits.forEach();

        processedUnits.addAll(compilationUnits);

        return false;
    }
}
