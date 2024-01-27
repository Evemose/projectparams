package org.projectparams.annotationprocessing.astcommons;

import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.projectparams.annotationprocessing.astcommons.parsing.ParsedExpression;

public class ExpressionMaker {

    private static TreeMaker treeMaker;
    @SuppressWarnings("unused")
    private static Names names;
    public static void init(TreeMaker treeMaker, Names names) {
        ExpressionMaker.treeMaker = treeMaker;
        ExpressionMaker.names = names;
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
        return switch (expr.type()) {
            case LITERAL -> {
                var tag = TypeUtils.geLiteralTypeTag(expr.name());
                yield makeLiteral(tag, TypeUtils.literalValueFromStr(tag, expr.name()));
            }
            case METHOD_INVOCATION -> throw new UnsupportedOperationException();
            case FIELD_ACCESS -> throw new UnsupportedOperationException();
            case NEW_CLASS -> throw new UnsupportedOperationException();
            default -> throw new IllegalStateException("Unexpected value: " + expr.type());
        };
    }
}
