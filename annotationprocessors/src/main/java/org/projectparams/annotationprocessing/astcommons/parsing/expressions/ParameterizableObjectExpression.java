package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;

/**
 * An expression that can be parameterized with type arguments.
 * Intended to be used for object, suitable for type application, i.e. FieldAccess or Identifier.
 */
public interface ParameterizableObjectExpression extends Expression {
    JCTree.JCExpression superToJcExpression();
}
