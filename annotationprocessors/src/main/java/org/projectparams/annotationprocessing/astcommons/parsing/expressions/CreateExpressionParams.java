package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.TypeTag;

import javax.annotation.Nullable;

/**
 * @param expression         The string representation of the expression
 * @param typeTag            The type tag of the expression
 * @param parsingContextPath The path to the enclosing invocable element (method, constructor, etc.)
 */
public record CreateExpressionParams(String expression,
                                     @Nullable TypeTag typeTag,
                                     TreePath parsingContextPath) {
    // name seems odd, but it`s needed to recursively create expressions
    public CreateExpressionParams withExpressionAndNullTag(String expression) {
        return new CreateExpressionParams(expression, null, parsingContextPath);
    }
}
