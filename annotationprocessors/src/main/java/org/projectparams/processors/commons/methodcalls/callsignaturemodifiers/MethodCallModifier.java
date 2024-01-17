package org.projectparams.processors.commons.methodcalls.callsignaturemodifiers;

import javax.lang.model.element.ExecutableElement;

@FunctionalInterface
public interface MethodCallModifier {

    /**
     * Modifies the all method calls for the given set of ExecutableElements.
     *
     *
     * @param methodElement the method element representing the method to modify
     */
    void modifyMethodCalls(ExecutableElement methodElement) throws Exception;
}
