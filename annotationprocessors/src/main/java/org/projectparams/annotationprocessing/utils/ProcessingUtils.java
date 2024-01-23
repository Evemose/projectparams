package org.projectparams.annotationprocessing.utils;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import org.projectparams.annotationprocessing.MainProcessor;
import org.projectparams.annotationprocessing.exceptions.ProcessingEnvironmentException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ProcessingUtils {
    private ProcessingUtils() {
        throw new UnsupportedOperationException();
    }

    public static JavacProcessingEnvironment getJavacProcessingEnvironment(Object procEnv)
            throws ProcessingEnvironmentException {
        if (procEnv instanceof ProcessingEnvironment processingEnv) {
            addOpensInModule(processingEnv);
        }
        if (procEnv instanceof JavacProcessingEnvironment jProcessingEnv) {
            return jProcessingEnv;
        }
        var delegate = tryGetDelegate(procEnv, "processingEnv").orElseThrow(() ->
                new ProcessingEnvironmentException("Could not get JavacProcessingEnvironment"));
        return getJavacProcessingEnvironment(delegate);
    }

    /**
     * this method opens required packages in jdk.compiler module to the current module
     */
    public static void addOpensInModule(ProcessingEnvironment processingEnv) {
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            // occurs in JDK 8 and below, then we don't need to do anything
            return;
        }

        var jdkCompilerModule = getJdkCompilerModule();
        var ownModule = getOwnModule();
        var requiredPackages = List.of(
                "com.sun.tools.javac.model",
                "com.sun.tools.javac.processing",
                "com.sun.tools.javac.tree",
                "com.sun.tools.javac.util",
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.comp"
        );
        try {
            // add required for project opens to jdk.compiler module
            var m = moduleClass.getDeclaredMethod("implAddOpens", String.class, moduleClass);
            m.setAccessible(true);
            requiredPackages.forEach(p -> {
                try {
                    m.invoke(jdkCompilerModule, p, ownModule);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Could not add opens to jdk.compiler module " + e.getMessage());
        }
    }

    private static Module getOwnModule() {
        try {
            var m = ReflectionUtils.getMethod(Class.class, "getModule");
            return (Module) m.invoke(MainProcessor.class);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("all")
    private static Module getJdkCompilerModule() {
        return ModuleLayer.boot().findModule("jdk.compiler").get();
    }


    /**
     * Attempts to get the delegate representing JavacProcessingEnvironment from the proxy delegateTo field.
     *
     * @param procEnv the ProcessingEnvironment instance
     * @return the delegate representing JavacProcessingEnvironment, or Optional.empty() if not found
     */
    private static Optional<Object> tryGetDelegate(Object procEnv, String... additionalFieldNames) {
        // 2 is the length of default field names
        final var allFieldNames = Arrays.copyOf(new String[]{"delegate", "val$delegateTo"}, 2 + additionalFieldNames.length);
        System.arraycopy(additionalFieldNames, 0, allFieldNames, 2, additionalFieldNames.length);

        for (var procEnvClass = procEnv.getClass(); procEnvClass != null; procEnvClass = procEnvClass.getSuperclass()) {
            Object delegate = null;
            for (var fieldName : allFieldNames) {
                try {
                    delegate = ReflectionUtils.getField(procEnvClass, fieldName).get(procEnv);
                    if (delegate != null) {
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
            if (delegate != null) return Optional.of(delegate);
        }

        return Optional.empty();
    }

    public static Element getRootPackage(RoundEnvironment roundEnv) {
        var rootElementsOpt = roundEnv.getRootElements().stream().findAny();
        if (rootElementsOpt.isEmpty()) {
            throw new RuntimeException("Could not find root package");
        }
        var rootElement = rootElementsOpt.get();
        while (!rootElement.getEnclosingElement().getKind().equals(ElementKind.MODULE)) {
            rootElement = rootElement.getEnclosingElement();
        }
        return rootElement;
    }

}
