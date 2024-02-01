package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory.Type;

/**
 * Test class for the Type enum specifically the 'of' method,
 * which determines the type of a given expression
 */
class ExpressionFactoryTest {
   /**
     * Test for the 'of' static method in the 'Type' Enum.
     */
    @Test
    void testOf() {
        assertEquals(Type.METHOD_INVOCATION, Type.of("method()"));
        assertEquals(Type.BINARY, Type.of("x + y"));
        assertEquals(Type.UNARY, Type.of("++x"));
        assertEquals(Type.FIELD_ACCESS, Type.of("obj.field"));
        assertEquals(Type.IDENTIFIER, Type.of("x"));
        assertEquals(Type.LITERAL, Type.of("10"));
        assertEquals(Type.TERNARY, Type.of("x ? y : z"));
        assertEquals(Type.PARENTHESIZED, Type.of("(xy)"));
        assertEquals(Type.CAST, Type.of("(SomeType.SomeNestedType) x"));
        assertEquals(Type.NEW_CLASS, Type.of("new Class()"));
    }
}