package org.projectparams.processors.commons.methodcalls.callscanners;

import org.projectparams.processors.commons.methodcalls.MethodCallSignature;
import spoon.Launcher;
import spoon.OutputType;
import spoon.SpoonModelBuilder;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.QueueProcessingManager;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DefaultMethodCallsScanner implements MethodCallsScanner {

    @Override
    public Set<CtInvocation<?>> getInvocationsInProject(MethodCallSignature methodCallSignature,
                                                        ProcessingEnvironment processingEnv,
                                                        Path srcDirPath) {
        var launcher = new Launcher();
        launcher.addInputResource(srcDirPath.toString());
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setAutoImports(true);
        launcher.buildModel();

        var methodCalls = new HashSet<CtInvocation<?>>();

        var processingManager = new QueueProcessingManager(launcher.getFactory());
        processingManager.addProcessor(new MethodCallScannerProcessor(methodCallSignature, methodCalls, processingEnv));

        processingManager.process(launcher.getFactory().Package().getRootPackage());
        var factory = launcher.getFactory();
        var literal = factory.createLiteral("testiculus");
        methodCalls.forEach(call -> {
            call.setArguments(Collections.singletonList(literal));
            launcher.addInputResource("C:/projectparams/test/src/main/java");
            //launcher.setSourceOutputDirectory("C:/projectparams/test/src/main/java");
            var compiler = launcher.createCompiler();
            compiler.setBinaryOutputDirectory(Path.of("C:/projectparams/test/build/classes/java/main").toFile());
            compiler.compile(SpoonModelBuilder.InputType.CTTYPES);
            //launcher.prettyprint();
        });
        return methodCalls;
    }

    private static class MethodCallScannerProcessor extends AbstractProcessor<CtClass<?>> {
        private final MethodCallSignature methodCallSignature;
        private final Set<CtInvocation<?>> methodCalls;
        private final ProcessingEnvironment processingEnv;
        private Map<String, Map<String, String>> localVarTypes;
        private Map<String, String> classLevelVarTypes;

        private MethodCallScannerProcessor(MethodCallSignature methodCallSignature, Set<CtInvocation<?>> methodCalls, ProcessingEnvironment processingEnv) {
            this.methodCallSignature = methodCallSignature;
            this.methodCalls = methodCalls;
            this.processingEnv = processingEnv;
        }

        @Override
        public void process(CtClass<?> ctClass) {
            classLevelVarTypes = getClassLevelVarTypes(ctClass);
            localVarTypes = getLocalVarTypes(ctClass);
            ctClass.getElements(new TypeFilter<>(CtInvocation.class)).stream()
                    .filter(this::doesMethodCallMatch)
                    .forEach(call -> {
                        processingEnv.getMessager().printMessage(
                                javax.tools.Diagnostic.Kind.NOTE,
                                "Found call: " + call.getExecutable().getSimpleName() + " in " +
                                        (call.getTarget() != null ?
                                                localVarTypes.get(call.getParent(CtMethod.class).getSimpleName()).containsKey(call.getTarget().toString()) ?
                                                        localVarTypes.get(call.getParent(CtMethod.class).getSimpleName()).get(call.getTarget().toString())
                                                        : classLevelVarTypes.get(call.getTarget().toString())
                                                : "notarget")
                        );
                        methodCalls.add((CtInvocation<?>) call);
                    });
        }

        private boolean doesMethodCallMatch(CtInvocation<?> ctInvocation) {
            return ctInvocation.getExecutable().getSimpleName().equals(methodCallSignature.methodName()) &&
                    getRealVarType((CtVariableRead<?>) ctInvocation.getTarget()).equals(methodCallSignature.className());
            //return true;

        }

        private Map<String, String> getClassLevelVarTypes(CtClass<?> ctClass) {
            return ctClass.getFields().stream()
                    .collect(HashMap::new,
                            (set, field) -> set.put(field.getSimpleName(), field.getType().getQualifiedName()),
                            HashMap::putAll);
        }

        private Map<String, String> getLocalVarTypesInMethod(CtMethod<?> ctMethod) {
            return ctMethod.getBody().getElements(new TypeFilter<>(CtLocalVariable.class)).stream()
                    .collect(HashMap::new,
                            (map, variable) -> {
                                if (!variable.getType().getSimpleName().equals("var")) {
                                    map.put(variable.getSimpleName(), variable.getType().getQualifiedName());
                                } else {
                                    map.put(variable.getSimpleName(), variable.getAssignment().getType().getQualifiedName());
                                }
                            },
                            HashMap::putAll);
        }

        private Map<String, Map<String, String>> getLocalVarTypes(CtClass<?> ctClass) {
            return ctClass.getMethods().stream()
                    .collect(HashMap::new,
                            (map, method) -> map.put(method.getSimpleName(), getLocalVarTypesInMethod(method)),
                            HashMap::putAll);
        }

        private String getRealVarType(CtVariableRead<?> variable) {
            var varName = variable.getVariable().getSimpleName();
            var methodName = variable.getParent(CtMethod.class).getSimpleName();
            if (localVarTypes.get(methodName).containsKey(varName)) {
                return localVarTypes.get(methodName).get(varName);
            } else {
                return classLevelVarTypes.get(varName);
            }
        }
    }
}
