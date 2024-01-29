package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.StatementTree;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Names;

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
        if (tag == TypeTag.BOOLEAN) {
            value = (boolean)value ? 1 : 0;
        }
        var literal = treeMaker.Literal(tag, value);
        literal.type = TypeUtils.getTypeByName(
                tag == TypeTag.BOOLEAN ? "java.lang.Boolean" : value.getClass().getCanonicalName()
        );
        return literal;
    }


    public static JCTree.JCFieldAccess makeFieldAccess(JCTree.JCExpression owner, String name) {
        return treeMaker.Select(owner, names.fromString(name));
    }

    // may be JCIdent or JCFieldAccess
    public static JCTree.JCExpression makeIdent(String name) {
        if (!name.contains(".")) {
            return treeMaker.Ident(names.fromString(name));
        } else {
            var expressionParts = name.split("\\.");
            var owner = makeIdent(expressionParts[0]);
            for (int i = 1; i < expressionParts.length - 1; i++) {
                owner = makeFieldAccess(owner, expressionParts[i]);
            }
            return makeFieldAccess(owner, expressionParts[expressionParts.length - 1]);
        }
    }

    public static JCTree.JCMethodInvocation makeMethodInvocation(
            JCTree.JCExpression methodSelect,
            JCTree.JCExpression... args) {
        return treeMaker.Apply(
                List.nil(),
                methodSelect,
                List.from(args)
        );
    }

    public static JCTree.JCNewClass makeNewClass(JCTree.JCExpression enclosing,
                                                 String className,
                                                 JCTree.JCExpression... args) {
        return treeMaker.NewClass(
                enclosing,
                List.nil(),
                makeIdent(className),
                List.from(args),
                null
        );
    }

    public static JCTree.JCStatement makeAssignment(JCTree.JCExpression variable, JCTree.JCExpression expression) {
        return treeMaker.Exec(treeMaker.Assign(variable, expression));
    }

    public static JCTree.JCBlock makeBlock(List<StatementTree> statements) {
        return treeMaker.Block(0, statements.stream().map(statement -> (JCTree.JCStatement) statement).collect(List.collector()));
    }
}
