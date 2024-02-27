package org.projectparams.annotationprocessing.astcommons.parsing.utils;

import com.sun.source.tree.MemberReferenceTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.Nullable;
import java.util.Objects;

public class ExpressionMaker {

    private static TreeMaker treeMaker;
    private static Names names;

    public static void init(TreeMaker treeMaker, Names names) {
        ExpressionMaker.treeMaker = treeMaker;
        ExpressionMaker.names = names;
    }

    public static JCTree.JCExpression makeTypeApply(JCTree.JCExpression expression, JCTree.JCExpression... typeArguments) {
        return treeMaker.TypeApply(expression, List.from(typeArguments));
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
            value = (boolean) value ? 1 : 0;
        }
        var literal = treeMaker.Literal(tag, value);
        literal.type = TypeUtils.getTypeByName(tag == TypeTag.BOOLEAN ? "boolean" : value.getClass().getCanonicalName());
        return literal;
    }


    public static JCTree.JCFieldAccess makeFieldAccess(JCTree.JCExpression owner, String name) {
        return treeMaker.Select(owner, makeName(name));
    }

    public static JCTree.JCExpression makeIdent(String name) {
        var topLevelDotIndex = ParsingUtils.getMatchingTopLevelSymbolLastIndex(name,
                ParsingUtils.equalsSymbolPredicate('.'));
        if (topLevelDotIndex != -1) {
            return treeMaker.Select(makeIdent(name.substring(0, topLevelDotIndex)),
                    makeName(name.substring(topLevelDotIndex + 1)));
        } else {
            return treeMaker.Ident(makeName(name));
        }
    }

    public static JCTree.JCMethodInvocation makeMethodInvocation(
            JCTree.JCExpression methodSelect,
            java.util.List<JCTree.JCExpression> typeArgs,
            JCTree.JCExpression... args) {
        return treeMaker.Apply(
                typeArgs == null ? null : List.from(typeArgs),
                methodSelect,
                List.from(args)
        );
    }

    public static JCTree.JCNewClass makeNewClass(JCTree.JCExpression enclosing,
                                                 String className,
                                                 java.util.List<JCTree.JCExpression> typeArgs,
                                                 JCTree.JCExpression... args) {
        return treeMaker.NewClass(
                enclosing,
                typeArgs == null ? null : List.from(typeArgs),
                makeIdent(className),
                List.from(args),
                null
        );
    }

    public static JCTree.JCAssign makeAssignment(JCTree.JCExpression variable, JCTree.JCExpression expression) {
        return treeMaker.Assign(variable, expression);
    }

    public static JCTree.JCStatement makeExpressionStatement(JCTree.JCExpression expression) {
        return treeMaker.Exec(expression);
    }

    public static Name makeName(String name) {
        return names.fromString(name);
    }

    public static JCTree.JCBlock makeBlock(java.util.List<JCTree.JCStatement> statements) {
        return treeMaker.Block(0, List.from(statements));
    }

    public static JCTree.JCMemberReference makeMemberReference(
            MemberReferenceTree.ReferenceMode mode,
            JCTree.JCExpression expression,
            String name,
            java.util.List<JCTree.JCExpression> typeArgs) {
        return treeMaker.Reference(mode, makeName(name), expression, typeArgs == null ? null : List.from(typeArgs));
    }

    public static JCTree.JCConditional makeConditional(JCTree.JCExpression condition,
                                                       JCTree.JCExpression trueExpression,
                                                       JCTree.JCExpression falseExpression) {
        return treeMaker.Conditional(condition, trueExpression, falseExpression);
    }

    public static JCTree.JCExpression makeBinary(JCTree.Tag tag,
                                                 JCTree.JCExpression left,
                                                 JCTree.JCExpression right) {
        return treeMaker.Binary(tag, left, right);
    }

    public static JCTree.JCExpression makeUnary(JCTree.Tag tag,
                                                JCTree.JCExpression expression) {
        return treeMaker.Unary(tag, expression);
    }

    public static JCTree.JCParens makeParens(JCTree.JCExpression expression) {
        return treeMaker.Parens(expression);
    }

    public static JCTree.JCTypeCast makeTypeCast(JCTree.JCExpression expression, String typeName) {
        return treeMaker.TypeCast(getTypeIdent(typeName), expression);
    }

    private static JCTree.JCExpression getTypeIdent(String typeName) {
        return switch (typeName) {
            case "short" -> treeMaker.TypeIdent(TypeTag.SHORT);
            case "byte" -> treeMaker.TypeIdent(TypeTag.BYTE);
            case "char" -> treeMaker.TypeIdent(TypeTag.CHAR);
            case "boolean" -> treeMaker.TypeIdent(TypeTag.BOOLEAN);
            case "float" -> treeMaker.TypeIdent(TypeTag.FLOAT);
            case "double" -> treeMaker.TypeIdent(TypeTag.DOUBLE);
            case "long" -> treeMaker.TypeIdent(TypeTag.LONG);
            case "int" -> treeMaker.TypeIdent(TypeTag.INT);
            default -> treeMaker.Ident(makeName(typeName));
        };
    }

    public static JCTree.JCArrayAccess makeArrayAccess(JCTree.JCExpression array, JCTree.JCExpression index) {
        return treeMaker.Indexed(array, index);
    }

    public static JCTree.JCNewArray makeNewArray(String type,
                                                 java.util.List<JCTree.JCExpression> dimensions,
                                                 java.util.List<JCTree.JCExpression> initializers) {
        var typeIdent = getTypeIdent(type);
        for (int i = 0; i < dimensions.size() - 1; i++) {
            typeIdent = treeMaker.TypeArray(typeIdent);
        }
        return treeMaker.NewArray(
                typeIdent,
                dimensions.stream().allMatch(Objects::isNull) ? List.nil() : List.from(dimensions),
                initializers == null ? null : List.from(initializers)
        );
    }

    public static JCTree.JCLambda makeLambda(
            java.util.List<JCTree.JCVariableDecl> parameters,
            JCTree body) {
        return treeMaker.Lambda(List.from(parameters), body);
    }

    public static JCTree.JCVariableDecl makeVariableDecl(String string, @Nullable Type type) {
        return treeMaker.VarDef(treeMaker.Modifiers(0), makeName(string),
                type == null ? null : makeIdent(type.toString()), null);
    }
}
