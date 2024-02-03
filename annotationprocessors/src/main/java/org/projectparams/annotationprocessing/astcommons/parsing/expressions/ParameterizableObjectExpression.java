package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;

public interface ParameterizableObjectExpression extends Expression {
    JCTree.JCExpression superToJcExpression();
}
