package org.projectparams.annotationprocessing.astcommons.parsing;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ParsedExpressionTest {

    /**
     * Class: ParsedExpression
     * Method: from
     * Method Description: Parses an input string `expression` to a corresponding `ParsedExpression` object.
     * The method identifies the type of the expression, extracts arguments if any and creates an instance of `ParsedExpression`.
     * Tests are written to validate these functionalities for different types of expressions.
     */

    @Test
    void testFrom_LiteralExpression() {
        var  expression = "true";
        var parsedExpression = ParsedExpression.from(expression);
        assertEquals(ParsedExpression.Type.LITERAL, parsedExpression.type());
        assertEquals(parsedExpression.name(), "true");
        assertNull(parsedExpression.owner());
        assertEquals(Collections.emptyList(), parsedExpression.arguments());
    }

    @Test
    void testFrom_VariableExpression() {
        var  expression = "myVariable";
        var parsedExpression = ParsedExpression.from(expression);
        assertEquals(ParsedExpression.Type.FIELD_ACCESS, parsedExpression.type());
        assertEquals("myVariable", parsedExpression.name());
        assertNull(parsedExpression.owner());
        assertEquals(Collections.emptyList(), parsedExpression.arguments());
    }

    @Test
    void testFrom_FieldAccessExpression() {
        var  expression = "myObject.myField";
        var parsedExpression = ParsedExpression.from(expression);
        assertEquals(ParsedExpression.Type.FIELD_ACCESS, parsedExpression.type());
        assertEquals("myField", parsedExpression.name());
        assertNotNull(parsedExpression.owner());
        assertEquals("myObject", parsedExpression.owner().name());
        assertEquals(Collections.emptyList(), parsedExpression.arguments());
    }

    @Test
    void testFrom_MethodInvocationExpression() {
        var  expression = "myObject.myMethod(arg1, arg2)";
        var parsedExpression = ParsedExpression.from(expression);
        assertEquals(ParsedExpression.Type.METHOD_INVOCATION, parsedExpression.type());
        assertEquals("myMethod", parsedExpression.name());
        assertNotNull(parsedExpression.owner());
        assertEquals("myObject", parsedExpression.owner().name());
        assertEquals(2, parsedExpression.arguments().size());
        assertEquals("arg1", parsedExpression.arguments().get(0).name());
        assertEquals("arg2", parsedExpression.arguments().get(1).name());
    }

    @Test
    void testFrom_NewClassExpression() {
        var  expression = "new MyClass(arg1, arg2)";
        var parsedExpression = ParsedExpression.from(expression);
        assertEquals(ParsedExpression.Type.NEW_CLASS, parsedExpression.type());
        assertEquals("MyClass", parsedExpression.name());
        assertNull(parsedExpression.owner());
        assertEquals(2, parsedExpression.arguments().size());
        assertEquals("arg1", parsedExpression.arguments().get(0).name());
        assertEquals(ParsedExpression.Type.FIELD_ACCESS, parsedExpression.arguments().get(0).type());
        assertEquals("arg2", parsedExpression.arguments().get(1).name());
        assertEquals(ParsedExpression.Type.FIELD_ACCESS, parsedExpression.arguments().get(1).type());
    }

    @Test
    void testFrom_ComplexExpression() {
        var  expression = "sosos.new myObject(new myObject(), totos.myMethod(sosos.arg3)).myMethod(arg1, arg2).myField";
        var parsedExpression = ParsedExpression.from(expression);

        assertEquals(ParsedExpression.Type.FIELD_ACCESS, parsedExpression.type());
        assertEquals("myField", parsedExpression.name());
        assertNotNull(parsedExpression.owner());

        var myMethod = parsedExpression.owner();
        assertEquals("myMethod", myMethod.name());
        assertNotNull(myMethod.owner());
        assertEquals(2, myMethod.arguments().size());
        assertEquals("arg1", myMethod.arguments().getFirst().name());
        assertEquals("arg2", myMethod.arguments().getLast().name());
        assertEquals(ParsedExpression.Type.METHOD_INVOCATION, parsedExpression.owner().type());

        var myObject = myMethod.owner();
        assertEquals("myObject", myObject.name());
        assertNotNull(myObject.owner());
        assertEquals(ParsedExpression.Type.NEW_CLASS, myObject.type());
        var myObjectArguments = myObject.arguments();
        assertEquals(2, myObjectArguments.size());
        assertEquals(ParsedExpression.Type.NEW_CLASS, myObjectArguments.getFirst().type());
        assertEquals("myObject", myObjectArguments.getFirst().name());
        assertThat(myObjectArguments.getFirst().arguments()).isEmpty();
        assertEquals(ParsedExpression.Type.METHOD_INVOCATION, myObjectArguments.getLast().type());
        assertEquals("myMethod", myObjectArguments.getLast().name());
        assertEquals(1, myObjectArguments.getLast().arguments().size());
        assertEquals("arg3", myObjectArguments.getLast().arguments().getFirst().name());
        assertEquals(ParsedExpression.Type.FIELD_ACCESS, myObjectArguments.getLast().arguments().getFirst().type());
        assertThat(myObjectArguments.getLast().arguments().getFirst().owner().name()).isEqualTo("sosos");


        var sosos = myObject.owner();
        assertEquals("sosos", sosos.name());
        assertNull(sosos.owner());
        assertEquals(ParsedExpression.Type.FIELD_ACCESS, sosos.type());
    }
}