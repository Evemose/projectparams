package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

public class TypeUtils {
    private static Trees trees;
    private static JavacTypes types;
    private static Elements elements;
    private static Symtab symtab;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Trees trees, JavacTypes types, Elements elements, Symtab symtab) {
        TypeUtils.trees = trees;
        TypeUtils.types = types;
        TypeUtils.elements = elements;
        TypeUtils.symtab = symtab;
    }

    public static Type getTypeByName(String name) {
        return (Type) switch (name) {
            case "int" -> symtab.intType;
            case "long" -> symtab.longType;
            case "float" -> symtab.floatType;
            case "double" -> symtab.doubleType;
            case "boolean" -> symtab.booleanType;
            case "void" -> symtab.voidType;
            default -> types.getDeclaredType(elements.getTypeElement(name));
        };
    }

    public static String getBoxedTypeName(String name) {
        return switch (name) {
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "float" -> "java.lang.Float";
            case "double" -> "java.lang.Double";
            case "boolean" -> "java.lang.Boolean";
            default -> name;
        };
    }

    public static TypeKind getTypeKind(TreePath path) {
        var type = trees.getTypeMirror(path);
        if (type == null) {
            return TypeKind.ERROR;
        }
        return trees.getTypeMirror(path).getKind();
    }

    public static String getOwnerTypeName(ExpressionTree expression, TreePath path) {
        if (expression instanceof MethodInvocationTree methodInvocationTree) {
            return getOwnerTypeName(methodInvocationTree, path);
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + expression.getClass().getCanonicalName());
        }
    }

    public static String getOwnerTypeName(MethodInvocationTree invocation, TreePath path) {
        String ownerQualifiedName;
        if (invocation.getMethodSelect() instanceof MemberSelectTree memberSelectTree) {
            ownerQualifiedName = getOwnerNameFromMemberSelect(memberSelectTree, path);
        } else if (invocation.getMethodSelect() instanceof IdentifierTree identifierTree) {
            ownerQualifiedName = getOwnerNameFromIdentifier(identifierTree, path);
        } else {
            throw new IllegalArgumentException("Unsupported method select type: "
                    + invocation.getMethodSelect().getClass().getCanonicalName());
        }
        return ownerQualifiedName;
    }

    // TODO: add support for IdentifierTree
    @SuppressWarnings("unused")
    private static String getOwnerNameFromIdentifier(IdentifierTree identifierTree, TreePath path) {
        return null;
    }

    private static String getOwnerNameFromMemberSelect(MemberSelectTree memberSelectTree, TreePath path) {
        var expression = memberSelectTree.getExpression();
        var ownerTree = trees.getTree(trees.getElement(new TreePath(path, expression)));
        String ownerQualifiedName = null;
        if (ownerTree != null) {
            if (ownerTree instanceof JCTree.JCVariableDecl varDecl) {
                var ownerType = varDecl.type;
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else if (ownerTree instanceof JCTree.JCMethodDecl methodDecl) {
                var ownerType = methodDecl.getReturnType();
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.type.toString());
                }
            } else {
                // TODO: remove placeholder when IdentifierTree is supported
                ownerQualifiedName = "";
            }
        }
        return ownerQualifiedName;
    }
}
