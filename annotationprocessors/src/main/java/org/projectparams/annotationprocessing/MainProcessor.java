package org.projectparams.annotationprocessing;

import com.google.auto.service.AutoService;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.processors.managers.DefaultProcessorsManager;
import org.projectparams.annotationprocessing.processors.managers.ProcessorsManager;
import org.projectparams.annotationprocessing.utils.ProcessingUtils;
import org.projectparams.annotationprocessing.utils.ReflectionUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Set;


// TODO: i guess this will need a migration from com.sun.tools.javac to jdk.compiler when works
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class MainProcessor extends AbstractProcessor {
    private JavacProcessingEnvironment javacProcessingEnv;
    private Trees trees;
    // Initialized in first round processing
    private ProcessorsManager processorsManager;
    // Initialized in first round of processing
    private Element rootPackage;
    private TreeMaker treeMaker;
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        try {
            super.init(processingEnv);
            this.javacProcessingEnv = ProcessingUtils.getJavacProcessingEnvironment(processingEnv);
            this.trees = Trees.instance(javacProcessingEnv);
            this.treeMaker = TreeMaker.instance(javacProcessingEnv.getContext());
            TypeUtils.init(trees, javacProcessingEnv.getTypeUtils(),
                    processingEnv.getElementUtils(),
                    Symtab.instance(javacProcessingEnv.getContext()));
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    t.getMessage() + "\n" + Arrays.toString(t.getStackTrace()).replaceAll(",", "\n"));
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (roundEnv.processingOver()) return false;
            if (rootPackage == null) {
                rootPackage = ProcessingUtils.getRootPackage(roundEnv);
                this.processorsManager =
                        new DefaultProcessorsManager(trees, treeMaker,
                                (PackageElement) rootPackage, processingEnv.getMessager());
            }
            processorsManager.process(roundEnv);
        } catch (Throwable t) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    t.getMessage() + "\n" + Arrays.toString(t.getStackTrace()).replaceAll(",", "\n"));
        }
        return false;
    }


    /**
     * Prevents the closing of the ClassLoader used by the processor.
     * This method wraps the initial ClassLoader in an anonymous proxy to prevent it from being closed.
     * The reason for wrapping it in an anonymous proxy is to ensure that the new ClassLoader is not a default instance
     * but an anonymous implementation.
     *
     * @throws RuntimeException if an exception occurs during the execution of the method
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
}
