package org.projectparams.processors.utils;

import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import org.projectparams.processors.exceptions.ProcessingEnvironmentException;

import java.util.Arrays;
import java.util.Optional;

@SuppressWarnings("deprecation")
public class ProcessingUtils {
    private ProcessingUtils() {
        throw new UnsupportedOperationException();
    }

    public static JavacProcessingEnvironment getJavacProcessingEnvironment(Object procEnv)
            throws ProcessingEnvironmentException {
        addOpensInModule();
        if (procEnv instanceof JavacProcessingEnvironment jProcessingEnv) {
            return jProcessingEnv;
        }
        var delegate = tryGetDelegate(procEnv, "processingEnv").orElseThrow(() ->
                new ProcessingEnvironmentException("Could not get JavacProcessingEnvironment"));
        return getJavacProcessingEnvironment(delegate);
    }

    public static JavacFiler getJavacFiler(Object filer) throws ProcessingEnvironmentException {
        if (filer instanceof JavacFiler) return (JavacFiler) filer;

        var delegate = tryGetDelegate(filer, "filer").orElseThrow(() ->
                new ProcessingEnvironmentException("Could not get JavacFiler"));
        return getJavacFiler(delegate);
    }
    public static void addOpensInModule() {
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            // occurs in JDK 8 and below, then we don't need to do anything
            return;
        }

        var jdkCompilerModule = getJdkCompilerModule();
        var ownModule = getOwnModule();
        var requiredPackages = new String[]{
                "com.sun.tools.javac.code",
                "com.sun.tools.javac.comp",
                "com.sun.tools.javac.file",
                "com.sun.tools.javac.main",
                "com.sun.tools.javac.model",
                "com.sun.tools.javac.parser",
                "com.sun.tools.javac.processing",
                "com.sun.tools.javac.tree",
                "com.sun.tools.javac.util",
                "com.sun.tools.javac.jvm",
        };

        try {
            // add required for project opens to jdk.compiler module
            var m = moduleClass.getDeclaredMethod("implAddOpens", String.class, moduleClass);
            ReflectionUtils.setFirstFieldVolatile(ownModule, true);
            for (var p : requiredPackages) m.invoke(jdkCompilerModule, p, ownModule);
        } catch (Exception ignore) {}
    }

    private static Module getOwnModule() {
        try {
            var m = ReflectionUtils.getMethod(Class.class, "getModule");
            return (Module) m.invoke(ProcessingUtils.class);
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

}
