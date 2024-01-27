package org.projectparams.annotationprocessing.astcommons;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.projectparams.annotationprocessing.astcommons.parsing.ParsedExpression;

import javax.annotation.processing.Messager;

public class ExpressionMaker {

    private static TreeMaker treeMaker;
    @SuppressWarnings("unused")
    private static Names names;
    private static Messager messager;
    public static void init(TreeMaker treeMaker, Names names, Messager messager) {
        ExpressionMaker.treeMaker = treeMaker;
        ExpressionMaker.names = names;
        ExpressionMaker.messager = messager;
    }


    @SuppressWarnings("unused")
    private static Object wrappedLiteral(Object defaultValue) {
        return switch (defaultValue) {
            case Short s -> defaultValue + "S";
            case Byte b -> defaultValue + "B";
            case Character c -> "'" + defaultValue + "'";
            case String s -> "\"" + defaultValue + "\"";
            case Float v -> defaultValue + "F";
            case null, default -> defaultValue;
        };
    }

    public static JCTree.JCExpression makeLiteral(TypeTag tag, Object value) {
        if (value == null) {
            return treeMaker.Literal(TypeTag.BOT, null);
        }
        // javac doesn't support short and byte literals directly, so we need to create them as ints and cast them
        if (tag == TypeTag.SHORT || tag == TypeTag.BYTE) {
            var cast = treeMaker.TypeCast(treeMaker.TypeIdent(tag), treeMaker.Literal(TypeTag.INT, value));
            cast.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
            return cast;
        }
        var literal = treeMaker.Literal(tag, value);
        if (tag == TypeTag.BOOLEAN) {
            literal.type = TypeUtils.getTypeByName("java.lang.Boolean");
        } else {
            literal.type = TypeUtils.getTypeByName(value.getClass().getCanonicalName());
        }
        return literal;
    }

    @SuppressWarnings("all")
    public static JCTree.JCExpression makeExpr(ParsedExpression expr) {
        messager.printMessage(javax.tools.Diagnostic.Kind.NOTE, "Making expr from " + expr);
        return switch (expr.type()) {
            case LITERAL -> {
                var tag = expr.returnType();
                yield makeLiteral(expr.returnType().getTag(),
                        TypeUtils.literalValueFromStr(expr.returnType().getTag(), expr.name()));
            }
            case METHOD_INVOCATION -> throw new UnsupportedOperationException();
            case FIELD_ACCESS -> makeFieldAccess(expr);
            case NEW_CLASS -> throw new UnsupportedOperationException();
            case IDENTIFIER -> {
                var name = names.fromString(expr.name());
                var ident = treeMaker.Ident(name);
                yield ident;
            }
            default -> throw new IllegalStateException("Unexpected value: " + expr.type());
        };
    }

    public static JCTree.JCExpression makeFieldAccess(ParsedExpression expr) {
        return treeMaker.Select(makeExpr(expr.owner()), names.fromString(expr.name()));
    }
}
