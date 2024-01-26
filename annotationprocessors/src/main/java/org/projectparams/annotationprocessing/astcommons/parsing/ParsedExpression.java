package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.tools.javac.util.List;

@SuppressWarnings("unused")
public record ParsedExpression(
        ParsedExpression.Type type,
        String name,
        List<ParsedExpression> arguments
) {
    public enum Type {
        NEW_CLASS,
        METHOD_INVOCATION,
        VARIABLE,
        LITERAL,
        FIELD_ACCESS
    }
}
