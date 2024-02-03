package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;

public interface ParameterizableExpression extends Expression {
    JCTree.JCExpression superToJcExpression();
}
