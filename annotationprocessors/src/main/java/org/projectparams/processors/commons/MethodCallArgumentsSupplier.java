package org.projectparams.processors.commons;

import com.sun.source.tree.ExpressionTree;

import java.util.List;

@FunctionalInterface
public interface MethodCallArgumentsSupplier {
    List<? extends ExpressionTree> getActualMethodCallArguments(List<? extends ExpressionTree> originalMethodCallArguments);
}
