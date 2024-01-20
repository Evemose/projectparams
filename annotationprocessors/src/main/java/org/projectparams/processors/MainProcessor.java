package org.projectparams.processors;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import org.projectparams.processors.ast.utils.ElementUtils;
import org.projectparams.processors.exceptions.ProcessingEnvironmentException;
import org.projectparams.processors.utils.ProcessingUtils;
import org.projectparams.processors.utils.ReflectionUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.sun.tools.javac.tree.JCTree.*;


// TODO: i guess this will need a migration from com.sun.tools.javac to jdk.compiler when works
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


    /**
     * Prevents the closing of the ClassLoader used by the processor.
     * This method wraps the initial ClassLoader in an anonymous proxy to prevent it from being closed.
     * The reason for wrapping it in an anonymous proxy is to ensure that the new ClassLoader is not a default instance
     * but an anonymous implementation.
     *
     * @throws RuntimeException     if an exception occurs during the execution of the method
     */
    @SuppressWarnings("all")
    private void preventClassLoaderFromClosing() {
        try {
            var f = ReflectionUtils.getField(JavacProcessingEnvironment.class, "processorClassLoader");
            var initialClassLoader = (ClassLoader) f.get(javacProcessingEnv);
            if (initialClassLoader == null) return;
            var wrapped = wrapInAnonymousProxy(initialClassLoader);
            f.set(javacProcessingEnv, wrapped);
        } catch (Throwable t) {
            // some versions of javac do not have processorClassLoader field, and, therefore, not closing it
            if (!(t instanceof NoSuchFieldException)) {
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * Provides a wrapper for the initial ClassLoader to prevent it from being closed.
     * Used in {@link #preventClassLoaderFromClosing()}.
     **/
    private ClassLoader wrapInAnonymousProxy(final ClassLoader classLoader) {
        return new ClassLoader() {
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                return classLoader.loadClass(name);
            }

            public String toString() {
                return classLoader.toString();
            }

            public URL getResource(String name) {
                return classLoader.getResource(name);
            }

            public Enumeration<URL> getResources(String name) throws IOException {
                return classLoader.getResources(name);
            }

            public InputStream getResourceAsStream(String name) {
                return classLoader.getResourceAsStream(name);
            }

            public void setDefaultAssertionStatus(boolean enabled) {
                classLoader.setDefaultAssertionStatus(enabled);
            }

            public void setPackageAssertionStatus(String packageName, boolean enabled) {
                classLoader.setPackageAssertionStatus(packageName, enabled);
            }

            public void setClassAssertionStatus(String className, boolean enabled) {
                classLoader.setClassAssertionStatus(className, enabled);
            }

            public void clearAssertionStatus() {
                classLoader.clearAssertionStatus();
            }
        };
    }

    private void setupFileManagerInterception() {
        preventClassLoaderFromClosing();

        // hope this won`t throw exception cause in lombok its accessed via reflection
        var originalFiler = javacProcessingEnv.getContext().get(JavaFileManager.class);

    }
}
