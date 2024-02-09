package org.projectparams.annotationprocessing.astcommons.parsing.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class ParsingUtilsTest {

    
    @ParameterizedTest
    @CsvSource({
        "'abc.def', '.', 3", 
        "'abcdefgh', '.', -1", 
        "'abc.def.ghi', '.', 3",
        "'ab.cde.f', '.', 2",
        "'aa(bb)', '(', 2",
        "'{xyz}', '{', 0",
        "'aa[bb]cc[dd]', '[', 2",
        "'boolean ? true : false', '?', 8"
    })
    public void testGetMatchingTopLevelSymbolIndex(String expression, char symbol, int expectedResult) {
        assertEquals(expectedResult, ParsingUtils.getMatchingTopLevelSymbolIndex(expression, ParsingUtils.equalsSymbolPredicate(symbol)));
    }

    @ParameterizedTest
    @CsvSource({
        "'abc.def', '.', 2, 3", 
        "'abcdefgh', '.', 2, -1",
        "'abc.def.ghi', '.', 5, 7",
        "'ab.cde.f', '.', 6, 6",
        "'aa(bb,cc)', '(', 4, -1",
        "'[[{xyz}]]', '{', 2, -1",
        "'aa[bb]cc[dd]', '[', 8, 8",
        "'boolean ? true : false', '?', 10, -1"
    })
    public void testGetMatchingTopLevelSymbolIndex_WithFromIndex(String expression, char symbol, int fromIndex, int expectedResult) {
        assertEquals(expectedResult, ParsingUtils.getMatchingTopLevelSymbolIndex(expression, ParsingUtils.equalsSymbolPredicate(symbol), fromIndex));
    }

    @ParameterizedTest
    @CsvSource({
            "'Hello.World', '.', 5",
            "'Hello  World', ' ', 6",
            "'import; java.util.List;', ';', 22",
            "'import java.util.List;', '.', 16",
            "'public class HelloWorld{  int a = 5; String str = \"Hello\"; }', ';', -1",
            "'public class \" HelloWorld{  int a = 5; String str = \\\"Hello\\\"; } \"', '\"', 65",
            "'public class HelloWorld{  int a = 5; String str = \"Hello\"; }', '=', -1",
            "'public void <T> doSomething(List<String> args) { }', '<', 12",
            "'public void doSomething(List<String> args) { }', '(', 23",
            "'public void doSomething() throws Exception { }', ' ', 42"
    })
    public void whenSymbolMatches_getMatchingTopLevelSymbolLastIndex_ReturnsCorrectIndex(String codeSnippet, char symbol, int expectedIndex) {
        assertEquals(expectedIndex, ParsingUtils.getMatchingTopLevelSymbolLastIndex(codeSnippet, ParsingUtils.equalsSymbolPredicate(symbol)));
    }

    @Test
    public void whenUnbalancedBracketsExist_getMatchingTopLevelSymbolLastIndex_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
                ParsingUtils.getMatchingTopLevelSymbolLastIndex("((Hello)", ParsingUtils.equalsSymbolPredicate('(')) );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo.bar",
            "foo().bar()",
            "1234.equals(1234)",
            "Math.PI",
            "Math.sin(3.14)",
            "\"foo.bar\".startsWith(\"foo\")",
            "java.lang.String.valueOf(3.14)",
            "\"3.14\".length()"
    })
    void testContainsTopLevelDot(String expression) {
        assertTrue(ParsingUtils.containsTopLevelDot(expression));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo(3.14)",
            "3.14",
            "\"hello.world\"",
            "foobar",
            ""
    })
    void testNotContainsTopLevelDot(String expression) {
        assertFalse(ParsingUtils.containsTopLevelDot(expression));
    }
}