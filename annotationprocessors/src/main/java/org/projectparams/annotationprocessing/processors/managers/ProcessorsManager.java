package org.projectparams.annotationprocessing.processors.managers;

import javax.annotation.processing.RoundEnvironment;

public interface ProcessorsManager {
    void process(RoundEnvironment roundEnv);
}
