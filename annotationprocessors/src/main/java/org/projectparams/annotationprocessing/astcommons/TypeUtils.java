package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.invocabletree.NewClassInvocableTree;
import org.projectparams.annotationprocessing.astcommons.parsing.CUContext;

import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

public class TypeUtils {
    private static Trees trees;
    private static JavacTypes types;
    private static Elements elements;
    private static Symtab symtab;
    private static Attr attr;
    private static Enter enter;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Trees trees, JavacTypes types, Elements elements, Symtab symtab, Attr attr, Enter enter) {
        TypeUtils.trees = trees;
        TypeUtils.types = types;
        TypeUtils.elements = elements;
        TypeUtils.symtab = symtab;
        TypeUtils.attr = attr;
        TypeUtils.enter = enter;
    }

    public static Type getTypeByName(String name) {
        return switch (name) {
            case "int" -> symtab.intType;
            case "long" -> symtab.longType;
            case "float" -> symtab.floatType;
            case "double" -> symtab.doubleType;
            case "boolean" -> symtab.booleanType;
            case "void" -> symtab.voidType;
            default -> {
                var typeElement = elements.getTypeElement(name);
                if (typeElement == null) {
                    throw new RuntimeException("Cannot resolve type for " + name);
                }
                var type = types.getDeclaredType(typeElement);
                yield (Type) type;
            }
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

    public static String getOwnerTypeName(InvocableTree tree, TreePath path) {
        if (tree.getWrapped() instanceof MethodInvocationTree methodInvocationTree) {
            return getOwnerTypeName(methodInvocationTree, path);
        } else if (tree.getWrapped() instanceof NewClassInvocableTree newClassInvocableTree) {
            var asJC = (JCTree.JCNewClass) newClassInvocableTree.getWrapped();
            var typeIdentifier = asJC.getIdentifier();
            return switch (typeIdentifier.getKind()) {
                case IDENTIFIER -> CUContext.from(path.getCompilationUnit()).importedClassNames().stream()
                        .filter(imp -> imp.endsWith("." + newClassInvocableTree.getOwnerTypeQualifiedName()))
                        .findAny().orElseThrow(() -> new RuntimeException("Cannot resolve owner type for "
                                + newClassInvocableTree.getOwnerTypeQualifiedName() + ": no matching import found"));
                case MEMBER_SELECT -> ((MemberSelectTree) typeIdentifier).toString();
                case PARAMETERIZED_TYPE -> ((ParameterizedTypeTree) typeIdentifier).getType().toString();
                default -> throw new UnsupportedOperationException("Type extraction not supported for trees of type " +
                        typeIdentifier.getKind());
            };
        } else {
            throw new IllegalArgumentException("Unsupported expression type: " + tree.getWrapped().getClass().getCanonicalName());
        }
    }

    public static String getOwnerTypeName(MethodInvocationTree invocation, TreePath path) {
        String ownerQualifiedName;
        if (invocation.getMethodSelect() instanceof MemberSelectTree memberSelectTree) {
            ownerQualifiedName = getOwnerNameFromMemberSelect(memberSelectTree, path);
//            var split = invocation.getMethodSelect().toString().split("\\.");
//            var methodName = split[split.length - 1];
//            if (methodName.equals("bibus") && !ownerQualifiedName.equals("<any>")) {
//                throw new RuntimeException("ownerQualifiedName: " + ownerQualifiedName + ", identifier: "
//                        + memberSelectTree.getExpression().toString() + ", invocation: " + invocation);
//            }
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
            } else if (ownerTree instanceof JCTree.JCClassDecl staticRef) {
                var ownerType = staticRef.sym.type;
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else {
                // TODO: remove placeholder when IdentifierTree is supported
                ownerQualifiedName = "";
            }
        } else {
            // in case owner is return type of fixed method, we won`t be able to access its tree
            // so retrieve type from method invocation manually
            if (expression instanceof JCTree.JCMethodInvocation methodInvocation) {
                if (methodInvocation.type != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(methodInvocation.type.toString());
//                    if (ownerQualifiedName.equals("org.projectparams.test.Abobus.abobus")) {
//                        throw new RuntimeException(methodInvocation.meth.type.getReturnType());
//                    }
                }
            } else {
                ownerQualifiedName = "";
            }
        }
        return ownerQualifiedName;
    }
}
