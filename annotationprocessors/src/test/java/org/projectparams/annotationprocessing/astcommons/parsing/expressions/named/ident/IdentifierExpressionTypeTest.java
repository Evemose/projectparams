package org.projectparams.annotationprocessing.astcommons.parsing.expressions.named.ident;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class IdentifierExpressionTypeTest {

    private final IdentifierExpressionType ieType = IdentifierExpressionType.getInstance();

    @ParameterizedTest
    @ValueSource(strings = {
            "validIdentifier",
            "AnotherValidIdentifier",
            "yetAnother",
            "newValidIdentifier",
            "latestIdentifier",
            "aboba"
    })
    void shouldMatchValidIdentifiers(String validIdentifier) {
        assertTrue(ieType.matches(validIdentifier));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "(invalidIdentifier)",
            "invalid.identifier",
            "array_access[]",
            "(invalidIdentifierNoClosing)",
            "invalidIdentifier{}",
            "conditional?invalidIdentifier:invalidIdentifier",
            "methodCall()",
            "methodCallWithArgs(arg1, arg2)",
            "new Class()",
            "new array[]{}",
            "++invalidIdentifier",
            "--invalidIdentifier",
            "some > invalidIdentifier",
    })
    void shouldNotMatchInvalidIdentifiers(String invalidIdentifier) {
        assertFalse(ieType.matches(invalidIdentifier));
    }
}