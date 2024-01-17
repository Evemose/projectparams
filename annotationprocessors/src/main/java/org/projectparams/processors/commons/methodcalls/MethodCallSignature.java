package org.projectparams.processors.commons.methodcalls;

import java.util.Optional;

public record MethodCallSignature(String className,
                                  String methodName,
                                  Class<?> returnType,
                                  Optional<String[]> parameterTypes) {
}
