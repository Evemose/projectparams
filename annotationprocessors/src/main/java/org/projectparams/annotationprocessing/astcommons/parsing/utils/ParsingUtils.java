package org.projectparams.annotationprocessing.astcommons.parsing.utils;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.operator.binary.BinaryExpressionType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ParsingUtils {
    private static int getArgsStartIndexFromIndex(String expression, char openingPar, char closingPar, int fromIndex) {
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
        return getArgsStartIndexFromIndex(expression, '(', ')', expression.length() - 1);
    }

    public static int getTypeArgsStartIndex(String expression) {
        var argsStartIndex = getArgsStartIndex(expression);
        if (argsStartIndex == -1) {
            argsStartIndex = expression.length() - 1;
        }
        return getArgsStartIndexFromIndex(expression, '<', '>', argsStartIndex);
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

    public static int getClosingParenthesesIndex(String expression, int fromIndex, char openingPar, char closingPar) {
        var parenthesesCount = 0;
        var i = fromIndex;
        while (i < expression.length()) {
            var c = expression.charAt(i);
            if (c == openingPar) {
                parenthesesCount++;
            } else if (c == closingPar) {
                parenthesesCount--;
                if (parenthesesCount == 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    public static int getClosingArgsParenthesesIndex(String expression, int fromIndex) {
        return getClosingParenthesesIndex(expression, fromIndex, '(', ')');
    }

    private static List<String> getArgStringsFromIndex(String expression, char openingPar, char closingPar, int fromIndex) {
        try {
            if (expression.endsWith(openingPar + "" + closingPar)) {
                return Collections.emptyList();
            }
            var argsString = expression.substring(
                    getArgsStartIndexFromIndex(expression, openingPar, closingPar, fromIndex) + 1,
                    expression.lastIndexOf(closingPar, fromIndex));
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
            throw new IllegalArgumentException("Unbalanced parentheses in " + expression + ": " + e.getMessage());
        }
    }

    public static List<String> getArgStrings(String expression, char openingPar, char closingPar) {
        return getArgStringsFromIndex(expression, openingPar, closingPar, expression.length()-1);
    }

    public static List<String> getTypeArgStrings(String expression) {
        var argsStartIndex = getArgsStartIndex(expression);
        if (argsStartIndex == -1) {
            argsStartIndex = expression.length() - 1;
        }
        return getArgStringsFromIndex(expression, '<', '>', argsStartIndex);
    }

    public static int getOwnerSeparatorIndex(String expression) {
        if (expression.matches("^\\s*new\\s+(\\w+\\.?)+\\s*\\(.*\\)\\s*")) {
            return -1;
        }
        var forwardWhitespacesCount = 0;
        while (forwardWhitespacesCount < expression.length() && Character.isWhitespace(expression.charAt(forwardWhitespacesCount))) {
            forwardWhitespacesCount++;
        }
        expression = expression.strip();
        var rightBound = getTypeArgsStartIndex(expression);
        if (rightBound == -1) {
            rightBound = getArgsStartIndex(expression);
            if (rightBound == -1) {
                rightBound = getArrayInitializerStartIndex(expression);
            }
        }
        return expression.lastIndexOf('.', rightBound == -1 ? expression.length() : rightBound) + forwardWhitespacesCount;
    }

    public static int getSelectedNewKeywordIndex(String expression) {
        var rightBound = getTypeArgsStartIndex(expression);
        if (rightBound == -1) {
            rightBound = getArgsStartIndex(expression);
        }
        expression = ' ' + expression;
        rightBound++;
        var matcher = Pattern.compile(new StringBuilder("W\\news\\").reverse().toString())
                .matcher(new StringBuilder(expression.substring(0, rightBound)).reverse());
        if (matcher.find()) {
            return expression.length() - matcher.end() - 1;
        }
        return -1;
    }

    public static int getArrayIndexStartIndex(String expression) {
        return getArgsStartIndexFromIndex(expression, '[', ']', expression.length() - 1);
    }

    public static int getArrayInitializerStartIndex(String expression) {
        return getArgsStartIndexFromIndex(expression, '{', '}', expression.length() - 1);
    }

    public static boolean containsTopLevelDot(String expression) {
        return getOwnerSeparatorIndex(expression) != -1;
    }

    public static List<String> getArrayDimensions(String expression) {
        var dimensionsStartIndex = expression.indexOf('[');
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
            dimensionsStartIndex = expression.indexOf('[', i+1);
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

}
