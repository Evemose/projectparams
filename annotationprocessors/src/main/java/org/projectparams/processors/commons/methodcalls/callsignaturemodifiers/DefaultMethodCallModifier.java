package org.projectparams.processors.commons.methodcalls.callsignaturemodifiers;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.bind.annotation.BindingPriority;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import org.projectparams.processors.commons.MethodCallArgumentsSupplier;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;

public class DefaultMethodCallModifier implements MethodCallModifier {
    private final MethodCallArgumentsSupplier methodCallArgumentsSupplier;
    private final ProcessingEnvironment processingEnv;

    public DefaultMethodCallModifier(MethodCallArgumentsSupplier methodCallArgumentsSupplier,
                                     ProcessingEnvironment processingEnv) {
        this.methodCallArgumentsSupplier = methodCallArgumentsSupplier;
        this.processingEnv = processingEnv;
    }

    @Override
    public void modifyMethodCalls(ExecutableElement methodElement) throws Exception {

        var type = processingEnv.getElementUtils().getTypeElement("org.projectparams.test.Main");
        var classLoader = ClassFileLocator.ForClassLoader.of(type.getClass().getClassLoader());
        var typePool = TypePool.Default.of(classLoader);
        var typeDescription = typePool.describe(type.getQualifiedName().toString()).resolve();

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "Modifying method calls " + methodElement.getSimpleName().toString()
        );

        var bud = new ByteBuddy()
                .redefine(typeDescription, classLoader)
                .visit(Advice.to(MethodCallInterceptor.class)
                        .on(ElementMatchers.named(methodElement.getSimpleName().toString())
                                .and(ElementMatchers
                                        .isDeclaredBy(ElementMatchers.named(
                                                methodElement.getEnclosingElement().toString()
                                                        .replaceAll("/", ".")
                                        )))
                        ))
                .make()
                .load(type.getClass().getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
        processingEnv.getMessager().printMessage(
                javax.tools.Diagnostic.Kind.NOTE,
                "Method calls modified " + bud
        );
    }

    public static class MethodCallInterceptor {
        @Advice.OnMethodEnter
        @RuntimeType
        @BindingPriority(BindingPriority.DEFAULT)
        public static Object intercept(@Advice.This(optional = true) Object target,
                                     @Advice.Origin MethodDescription method) {
            // Implement your custom logic to intercept and modify method calls
            // You can replace the method call or perform other modifications here
            return "abobusBobobus";
        }
    }
}
