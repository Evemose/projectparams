package org.projectparams.annotationprocessing.exceptions;

import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

public class UnsupportedSignatureException extends Exception {
    private final String missingArgumentType;
    private final int argumentIndex;
    private final InvocableInfo invocableInfo;

    public UnsupportedSignatureException(String missingArgumentType, int argumentIndex, InvocableInfo invocableInfo) {
        super();
        this.missingArgumentType = missingArgumentType;
        this.argumentIndex = argumentIndex;
        this.invocableInfo = invocableInfo;
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
                + " in method invocation " + invocableInfo;
    }
}
