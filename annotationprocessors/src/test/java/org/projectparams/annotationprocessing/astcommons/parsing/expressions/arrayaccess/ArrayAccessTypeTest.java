package org.projectparams.annotationprocessing.astcommons.parsing.expressions.arrayaccess;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class is used to run tests on the ArrayAccessType class's methods.
 */
public class ArrayAccessTypeTest {

    ArrayAccessType arrayAccessType = ArrayAccessType.getInstance();


    @ParameterizedTest
    @ValueSource(strings = {
            "myArray[3]",
            "matrix[2][3]",
            "tensor[2][4][3]",
            "numbers[getindex()[4]]",
            "myArray[someMethod(param1[3], param2[2])]"
    })
    public void parametrizedTestMatchesTrue(String input){
        assertTrue(arrayAccessType.matches(input));
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "myVar",
            "calculateValue()",
            "String text = \"some text\"",
            "new Object(arr[3])",
            "method(param1, param2)"
    })
    public void parametrizedTestMatchesFalse(String input){
        assertFalse(arrayAccessType.matches(input));
    }
}