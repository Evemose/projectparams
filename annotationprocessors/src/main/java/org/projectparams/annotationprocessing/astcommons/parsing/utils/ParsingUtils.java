package org.projectparams.annotationprocessing.astcommons.parsing.utils;

import com.sun.tools.javac.tree.JCTree;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// this class contains complex algorithmic logic and i strongly not recommend to try to optimize or refactor it
// unless you are sure that you understand the logic and the purpose of the code
public class ParsingUtils {
    public static int getCorrespondingOpeningParIndex(String expression, char openingPar, char closingPar, int fromIndex) {
        var i = fromIndex;
        var parenthesesCount = 0;
        while (i >= 0) {
            var c = expression.charAt(i);
            if (c == closingPar) {
                parenthesesCount++;
            } else if (c == openingPar) {
                if (parenthesesCount == 1) {
                    return i;
                }
                parenthesesCount--;
            }
            i--;
        }
        return -1;
    }

    public static int getArgsStartIndex(String expression) {
        if (!expression.endsWith(")")) {
            return -1;
        }
        return getCorrespondingOpeningParIndex(expression, '(', ')', expression.length() - 1);
    }

    public static int getTypeArgsStartIndex(String expression) {
        var argsStartIndex = getArgsStartIndex(expression);
        if (argsStartIndex == -1) {
            argsStartIndex = expression.length() - 1;
        }
        return getCorrespondingOpeningParIndex(expression, '<', '>', argsStartIndex);
    }

    public static String getStringOfOperator(JCTree.Tag operatorTag) {
        return switch (operatorTag) {
            case PLUS, POS -> "+";
            case MINUS, NEG -> "-";
            case MUL -> "*";
            case DIV -> "/";
            case MOD -> "%";
            case BITAND -> "&";
            case BITOR -> "|";
            case BITXOR -> "^";
            case SL -> "<<";
            case SR -> ">>";
            case USR -> ">>>";
            case LT -> "<";
            case GT -> ">";
            case LE -> "<=";
            case GE -> ">=";
            case EQ -> "==";
            case NE -> "!=";
            case AND -> "&&";
            case OR -> "||";
            case TYPETEST -> "instanceof";
            case NOT -> "!";
            case COMPL -> "~";
            case PREINC -> "++";
            case PREDEC -> "--";
            case POSTINC -> "++";
            case POSTDEC -> "--";
            default -> throw new IllegalArgumentException("Unknown operator tag " + operatorTag);
        };
    }

    private static List<String> getArgStringsFromIndex(String expression, char openingPar, char closingPar, int fromIndex) {
        try {
            if (expression.endsWith(openingPar + "" + closingPar)) {
                return Collections.emptyList();
            }
            var argsString = expression.substring(
                    getCorrespondingOpeningParIndex(expression, openingPar, closingPar, fromIndex) + 1,
                    expression.lastIndexOf(closingPar, fromIndex));
            if (argsString.isBlank()) {
                return Collections.emptyList();
            }
            var parenthesesCount = 0;
            var args = new ArrayList<String>();
            var argBeginIndex = 0;
            for (var i = 0; i < argsString.length(); i++) {
                var c = argsString.charAt(i);
                if (c == openingPar) {
                    parenthesesCount++;
                } else if (c == closingPar) {
                    parenthesesCount--;
                } else if (c == ',' && parenthesesCount == 0) {
                    if (argBeginIndex == i || argsString.substring(argBeginIndex, i).matches("\\s*")) {
                        throw new IllegalArgumentException("Empty argument in " + expression);
                    }
                    args.add(argsString.substring(argBeginIndex, i));
                    argBeginIndex = i + 1;
                }
            }
            if (parenthesesCount != 0) {
                throw new IllegalArgumentException("Unbalanced parentheses in " + expression + " at " + argsString);
            }
            if (argBeginIndex == argsString.length()) {
                throw new IllegalArgumentException("Empty argument in " + expression);
            }
            args.add(argsString.substring(argBeginIndex));
            return args;
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unbalanced parentheses in " + expression, e);
        }
    }

    public static List<String> getArgStrings(String expression, char openingPar, char closingPar) {
        return getArgStringsFromIndex(expression, openingPar, closingPar, expression.length() - 1);
    }

    public static List<String> getTypeArgStrings(String expression) {
        var argsStartIndex = getArgsStartIndex(expression);
        if (argsStartIndex == -1) {
            argsStartIndex = expression.length() - 1;
        }
        return getArgStringsFromIndex(expression, '<', '>', argsStartIndex);
    }

    public static int getOwnerSeparatorIndex(String expression) {
        return getMatchingTopLevelSymbolLastIndex(expression, equalsSymbolPredicate('.'));
    }

    public static int getSelectedNewKeywordIndex(String expression) {
        return getMatchingTopLevelSymbolLastIndex(expression, (expr, index) -> expr.substring(index).startsWith("new"));
    }

    public static int getArrayIndexStartIndex(String expression) {
        return getCorrespondingOpeningParIndex(expression, '[', ']', expression.length() - 1);
    }

    public static int getArrayInitializerStartIndex(String expression) {
        return getCorrespondingOpeningParIndex(expression, '{', '}', expression.length() - 1);
    }

    public static boolean containsTopLevelDot(String expression) {
        return getMatchingTopLevelSymbolIndex(expression, equalsSymbolPredicate('.')
                .and(ParsingUtils::isNotPartOfLiteral)) != -1;
    }

    private static boolean isNotPartOfLiteral(String expr, int idx) {
        return idx > 0 && !expr.matches(".{" + idx + "}\\.(\\d|[fFdD])\\d*[fFdD]?.*");
    }

    public static List<String> getArrayDimensions(String expression) {
        var dimensionsStartIndex = expression.indexOf('[', ParsingUtils.getArgsStartIndex(expression) + 1);
        if (dimensionsStartIndex == -1) {
            throw new IllegalArgumentException("No array dimensions in " + expression);
        }
        var dimensions = new ArrayList<String>();
        do {
            var parenthesesCount = 0;
            var i = dimensionsStartIndex;
            for (; i < expression.length(); i++) {
                var c = expression.charAt(i);
                if (c == '[') {
                    parenthesesCount++;
                } else if (c == ']') {
                    if (parenthesesCount == 1) {
                        dimensions.add(expression.substring(dimensionsStartIndex + 1, i));
                        break;
                    }
                    parenthesesCount--;
                }
            }
            if (i == expression.length()) {
                throw new IllegalArgumentException("Unbalanced parentheses in " + expression);
            }
            dimensionsStartIndex = expression.indexOf('[', i + 1);
        } while (dimensionsStartIndex != -1);
        return dimensions;
    }

    public static List<String> getArrayInitializerExpressions(String expression) {
        var arrayInitializerStartIndex = getArrayInitializerStartIndex(expression);
        if (arrayInitializerStartIndex == -1) {
            throw new IllegalArgumentException("No array initializer in " + expression);
        }
        return getArgStringsFromIndex(expression, '{', '}', expression.length() - 1);

    }


    /**
     * Returns the index of the matching top-level symbol in the given expression starting from the specified index.
     * A top-level symbol is a symbol that is not enclosed in parentheses, brackets, braces, angle brackets or conditional expression.
     * The method searches for the first matching symbol from the specified index to the end of the expression.
     * The method should only be invoked for strings where any of listed above enclosers must be balanced.
     *
     * @param expression      the expression to search in
     * @param symbolPredicate the predicate that determines if a symbol matches
     * @param fromIndex       the index to start the search from (inclusive)
     * @param direction       the direction to search in, true for forward, false for backward
     * @return the index of the matching top-level symbol, or -1 if no match is found
     * @throws IndexOutOfBoundsException if the starting index is out of bounds for the expression
     * @throws IllegalArgumentException  if the parentheses in the expression are unbalanced
     */
    private static int getMatchingTopLevelSymbolIndex(String expression, BiPredicate<String, Integer> symbolPredicate, int fromIndex, boolean direction) {
        validateFromIndex(expression, fromIndex);
        var enclosersCount = getEnclosersCountMap();
        boolean mutated;
        var couldBeChecked = false;
        var newKeywordEnclState = new HashSet<>(enclosersCount.entrySet());
        var lambdaEnclState = new HashSet<>(enclosersCount.entrySet());
        Integer capture = null;
        for (var i = 0; i < expression.length(); i++) {
            couldBeChecked = couldBeChecked || updateCouldBeChecked(fromIndex, direction, i);
            if (mayUpdateCapture(direction, capture)) {
                capture = updateCapture(expression, symbolPredicate, couldBeChecked, enclosersCount, i).orElse(capture);
            }
            mutated = updateEnclosers(expression, i, enclosersCount, newKeywordEnclState, lambdaEnclState);
            if (enclosersCount.values().stream().anyMatch(val -> val < 0)) {
                throw new IllegalArgumentException("Unbalanced parentheses in " + expression);
            }
            // need to check once more after modifying enclosers count in case matched symbol is encloser
            if (mutated && mayUpdateCapture(direction, capture) && couldBeChecked
                    && isNotEnclosed(enclosersCount) && symbolPredicate.test(expression, i)) {
                capture = i;
            }
        }
        if (isNotEnclosed(enclosersCount)) {
            return Objects.requireNonNullElse(capture, -1);
        }
        throw new IllegalArgumentException("Unclosed enclosing expressions in " + expression
                + ". Non-empty enclosers: " + enclosersCount.entrySet().stream().filter(e -> e.getValue() != 0).toList());
    }

    private static boolean mayUpdateCapture(boolean direction, Integer capture) {
        return capture == null || !direction;
    }

    private static boolean updateEnclosers(String expression,
                                           int i,
                                           HashMap<Character, Integer> enclosersCount,
                                           Set<Map.Entry<Character, Integer>> newKeywordEnclState,
                                           Set<Map.Entry<Character, Integer>> lambdaEnclState) {
        var c = expression.charAt(i);
        var mutated = false;
        if (isOpeningPar(c) && (c != '<' || isTypeArgsBracket(expression, i)) || c == '?') {
            enclosersCount.merge(c, 1, Integer::sum);
            mutated = true;
        } else if (c == ']' || c == '}') {
            enclosersCount.merge((char) (c - 2), -1, Integer::sum);
            mutated = true;
            updateNewKeywordEnclCount(enclosersCount, newKeywordEnclState);
        } else if (c == ')') {
            // opening bracket has code 40, closing bracket has code 41,
            // while other brackets have offset 2 between opening and closing bracket
            // so this case is exceptional
            enclosersCount.merge('(', -1, Integer::sum);
            mutated = true;
            updateNewKeywordEnclCount(enclosersCount, newKeywordEnclState);
        } else if (c == '>') {
            if (isTypeArgsBracket(expression, i)) {
                enclosersCount.merge('<', -1, Integer::sum);
                mutated = true;
            } else if (i > 0 && expression.charAt(i - 1) == '-') {
                enclosersCount.merge('l', 1, Integer::sum);
                mutated = true;
            }
        } else if (c == ':') {
            mutated = updateConditionalEnclCount(expression, i, enclosersCount);
        } else if (expression.matches(".{" + i + "}new\\s.*") && enclosersCount.get('n') == 0) {
            enclosersCount.merge('n', 1, Integer::sum);
            newKeywordEnclState.clear();
            newKeywordEnclState.addAll(enclosersCount.entrySet());
            mutated = true;
        } else if (c == '"') {
            if (i == 0 || expression.charAt(i - 1) != '\\') {
                if (enclosersCount.get(c) == 0) {
                    enclosersCount.merge(c, 1, Integer::sum);
                } else {
                    enclosersCount.merge(c, -1, Integer::sum);
                }
                mutated = true;
            }
        } else if (c == ',' && isNotEnclosed(enclosersCount)) {
            if (enclosersCount.get('l') > 0) {
                enclosersCount.merge('l', -1, Integer::sum);
                lambdaEnclState.clear();
                lambdaEnclState.addAll(enclosersCount.entrySet());
                mutated = true;
            }
        }
        return mutated;
    }

    private static boolean updateConditionalEnclCount(String expression, int i, HashMap<Character, Integer> enclosersCount) {
        if (i == 0 || i == expression.length() - 1) {
            throw new IllegalArgumentException("Dangling colon in " + expression);
        } else if (expression.charAt(i - 1) != ':' && expression.charAt(i + 1) != ':') { // exclude :: operator
            enclosersCount.merge('?', -1, Integer::sum);
            return true;
        }
        return false;
    }

    private static void updateNewKeywordEnclCount(HashMap<Character, Integer> enclosersCount, Set<Map.Entry<Character, Integer>> newKeywordEnclState) {
        if (enclosersCount.get('n') > 0 && doesEndNewKeywordStatement(enclosersCount, newKeywordEnclState)) {
            enclosersCount.merge('n', -1, Integer::sum);
        }
    }

    private static HashMap<Character, Integer> getEnclosersCountMap() {
        return new HashMap<>(Map.of(
                '(', 0,
                '[', 0,
                '{', 0,
                '<', 0,
                '"', 0,
                '?', 0, // conditional expression
                'n', 0, // new keyword
                'l', 0 // lambda
        ));
    }

    private static Optional<Integer> updateCapture(String expression,
                                                   BiPredicate<String, Integer> symbolPredicate,
                                                   boolean couldBeChecked,
                                                   HashMap<Character, Integer> enclosersCount,
                                                   int i) {
        if (couldBeChecked && isNotEnclosed(enclosersCount) && symbolPredicate.test(expression, i)) {
            return Optional.of(i);
        }
        return Optional.empty();
    }

    private static boolean updateCouldBeChecked(int fromIndex, boolean direction, int i) {
        return direction ? i >= fromIndex : i <= fromIndex;
    }

    private static void validateFromIndex(String expression, int fromIndex) {
        if (fromIndex == 0 && expression.isEmpty()) {
            return;
        }
        if (fromIndex >= expression.length() || fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex " + fromIndex + " is out of bounds for " + expression);
        }
    }

    private static boolean doesEndNewKeywordStatement(HashMap<Character, Integer> enclosersCount,
                                                      Set<Map.Entry<Character, Integer>> newKeywordEnclState) {
        return enclosersCount.entrySet().containsAll(newKeywordEnclState)
                && newKeywordEnclState.containsAll(enclosersCount.entrySet());
    }

    private static boolean isNotEnclosed(HashMap<Character, Integer> enclosersCount) {
        return enclosersCount.values().stream().allMatch(Predicate.isEqual(0));
    }


    /**
     * Returns the index of the matching top-level symbol in the given expression starting from the specified index.
     * A top-level symbol is a symbol that is not enclosed in parentheses, brackets, braces, angle brackets or conditional expression.
     * The method searches for the first matching symbol from the specified index to the end of the expression.
     * The method should only be invoked for strings where any of listed above enclosers must be balanced.
     *
     * @param expression      the expression to search in
     * @param symbolPredicate the predicate that determines if a symbol matches
     * @param fromIndex       the index to start the search from (inclusive)
     * @return the index of the matching top-level symbol, or -1 if no match is found
     * @throws IndexOutOfBoundsException if the starting index is out of bounds for the expression
     * @throws IllegalArgumentException  if the parentheses in the expression are unbalanced
     */
    public static int getMatchingTopLevelSymbolIndex(String expression, BiPredicate<String, Integer> symbolPredicate, int fromIndex) {
        return getMatchingTopLevelSymbolIndex(expression, symbolPredicate, fromIndex, true);
    }

    /**
     * Returns the index of the matching top-level symbol in the given expression starting from the specified index.
     * A top-level symbol is a symbol that is not enclosed in parentheses, brackets, braces, angle brackets or conditional expression.
     * The method searches for the first matching symbol through whole expression from start to end of the expression.
     * The method should only be invoked for strings where any of listed above enclosers must be balanced.
     *
     * @param expression      the expression to search in
     * @param symbolPredicate the predicate that determines if a symbol matches
     * @return the index of the matching top-level symbol, or -1 if no match is found
     * @throws IndexOutOfBoundsException if the starting index is out of bounds for the expression
     * @throws IllegalArgumentException  if the parentheses in the expression are unbalanced
     */
    public static int getMatchingTopLevelSymbolIndex(String expression, BiPredicate<String, Integer> symbolPredicate) {
        return getMatchingTopLevelSymbolIndex(expression, symbolPredicate, 0, true);
    }

    /**
     * Returns the index of the matching top-level symbol in the given expression starting from the specified index.
     * A top-level symbol is a symbol that is not enclosed in parentheses, brackets, braces, angle brackets or conditional expression.
     * The method searches for the first matching symbol from the end of expression to its start.
     * The method should only be invoked for strings where any of listed above enclosers must be balanced.
     *
     * @param expression      the expression to search in
     * @param symbolPredicate the predicate that determines if a symbol matches
     * @return the index of the matching top-level symbol, or -1 if no match is found
     * @throws IndexOutOfBoundsException if the starting index is out of bounds for the expression
     * @throws IllegalArgumentException  if the parentheses in the expression are unbalanced
     */
    public static int getMatchingTopLevelSymbolLastIndex(String expression, BiPredicate<String, Integer> symbolPredicate) {
        return getMatchingTopLevelSymbolIndex(expression, symbolPredicate, expression.length() - 1, false);
    }

    public static int countMatchingTopLevelSymbols(String expression, BiPredicate<String, Integer> symbolPredicate) {
        var count = 0;
        var fromIndex = 0;
        while (fromIndex != -1) {
            fromIndex = getMatchingTopLevelSymbolIndex(expression, symbolPredicate, fromIndex);
            if (fromIndex != -1) {
                count++;
                fromIndex++;
            }
        }
        return count;
    }

    private static boolean isOpeningPar(char c) {
        return c == '(' || c == '[' || c == '{' || c == '<';
    }

    public static boolean isTypeArgsBracket(String expression, int charIndex) {
        if (expression.charAt(charIndex) == '<') {
            var closeBracketIndex = expression.indexOf('>', charIndex);
            if (closeBracketIndex == -1) {
                return false;
            }
            var closeParIndex = expression.indexOf(')', charIndex);
            return closeParIndex == -1 || closeParIndex > closeBracketIndex;
        } else if (expression.charAt(charIndex) == '>') {
            if (charIndex > 0 && expression.charAt(charIndex - 1) == '-') {
                return false;
            }
            var openBracketIndex = expression.lastIndexOf('<', charIndex);
            if (openBracketIndex == -1) {
                return false;
            }
            var closeParIndex = expression.lastIndexOf(')', charIndex);
            return closeParIndex == -1 || closeParIndex < openBracketIndex;
        } else {
            return false;
        }
    }

    public static BiPredicate<String, Integer> equalsSymbolPredicate(char symbol) {
        return (expr, index) -> expr.charAt(index) == symbol;
    }

    public static List<String> getTypeParameters(String expression) {
        var typeParameters = new ArrayList<String>();
        if (hasTypeArgs(expression)) {
            var typeArgsParts = getTypeArgStrings(expression);
            for (var typeArg : typeArgsParts) {
                typeParameters.add(typeArg.strip());
            }
        }
        return typeParameters;
    }

    private static boolean hasTypeArgs(String expression) {
        return getMatchingTopLevelSymbolLastIndex(expression, ParsingUtils::isTypeArgsBracket) > getOwnerSeparatorIndex(expression);
    }
}
