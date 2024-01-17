package org.projectparams.processors.commons.methodcalls.callscanners;

import org.projectparams.processors.commons.methodcalls.MethodCallSignature;
import spoon.reflect.code.CtInvocation;

import javax.annotation.processing.ProcessingEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public interface MethodCallsScanner {
    Set<CtInvocation<?>> getInvocationsInProject(MethodCallSignature methodCallSignature,
                                                 ProcessingEnvironment processingEnv,
                                                 Path srcDirPath) throws IOException;
}
