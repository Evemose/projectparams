package org.projectparams.annotationprocessing.exceptions;

import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;

public class UnsupportedSignatureException extends Exception {
    private final String missingArgumentType;
    private final int argumentIndex;
    private final MethodInfo methodInfo;

    public UnsupportedSignatureException(String missingArgumentType, int argumentIndex, MethodInfo methodInfo) {
        super();
        this.missingArgumentType = missingArgumentType;
        this.argumentIndex = argumentIndex;
        this.methodInfo = methodInfo;
    }

    @SuppressWarnings("unused")
    public String getMissingArgumentType() {
        return missingArgumentType;
    }

    @SuppressWarnings("unused")
    public int getArgumentIndex() {
        return argumentIndex;
    }

    @Override
    public String getMessage() {
        return "Unsupported signature: missing required argument " + missingArgumentType + " at index " + argumentIndex
                + " in method invocation " + methodInfo;
    }
}
