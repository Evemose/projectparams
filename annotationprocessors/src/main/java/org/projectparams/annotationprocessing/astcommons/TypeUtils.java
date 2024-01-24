package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.JCTree;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
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
                    throw new IllegalArgumentException("Cannot resolve type for " + name);
                }
                var type = types.getDeclaredType(typeElement);
                yield (Type) type;
            }
        };
    }


    public static void updateIdentifierType(NewClassTree newClassTree, Type newType) {
        var asJC = (JCTree.JCNewClass) newClassTree;
        asJC.clazz.type = newType;
        asJC.constructorType = new Type.MethodType(
                com.sun.tools.javac.util.List.from(
                        newClassTree.getArguments().stream().map(arg -> ((JCTree.JCExpression) arg).type).toList()),
                TypeUtils.getTypeByName("void"),
                com.sun.tools.javac.util.List.nil(),
                asJC.clazz.type.tsym);
        asJC.type = newType;
        asJC.constructor = asJC.clazz.type.tsym;
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
    public static String getOwnerNameFromIdentifier(IdentifierTree identifierTree, TreePath path) {
        while (path != null && !(path.getLeaf() instanceof ClassTree)) {
            path = path.getParentPath();
        }
        return path != null ? getFullyQualifiedName((ClassTree) path.getLeaf()) : null;
    }

    public static String getFullyQualifiedName(ClassTree classTree) {
        JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) classTree;
        Element classElement = classDecl.sym;
        PackageElement packageElement = elements.getPackageOf(classElement);
        return packageElement.getQualifiedName().toString() + "." + classElement.getSimpleName().toString();
    }

    public static void attributeExpression(JCTree.JCExpression expression, TreePath path) {
        var env = enter.getTopLevelEnv((JCTree.JCCompilationUnit) path.getCompilationUnit());
        attr.attribExpr(expression, env);
    }

    private static String getOwnerNameFromMemberSelect(MemberSelectTree memberSelectTree, TreePath path) {
        var expression = memberSelectTree.getExpression();
        var ownerTree = trees.getTree(trees.getElement(new TreePath(path, expression)));
        String ownerQualifiedName = null;
        if (ownerTree != null) {
            if (ownerTree instanceof JCTree.JCClassDecl staticRef) {
                var ownerType = staticRef.sym.type;
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else if (ownerTree instanceof JCTree.JCExpression newClass) {
                var ownerType = newClass.type;
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else if (ownerTree instanceof JCTree.JCVariableDecl varDecl) {
                var ownerType = varDecl.type;
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else {
                throw new IllegalArgumentException("Unsupported owner type: " + ownerTree.getClass().getCanonicalName());
            }
        } else {
            // in case owner is return type of fixed method, we won`t be able to access its tree
            // so retrieve type from method invocation manually
            if (expression instanceof JCTree.JCMethodInvocation methodInvocation) {
                if (methodInvocation.type != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(methodInvocation.type.toString());
//                    if (possibleOwnerQualifiedNames.equals("org.projectparams.test.Abobus.abobus")) {
//                        throw new RuntimeException(methodInvocation.meth.type.getReturnType());
//                    }
                }
            } else if (expression instanceof JCTree.JCNewClass newClass) {
                var ownerType = newClass.constructorType.tsym.type;
                if (ownerType != null) {
                    ownerQualifiedName = TypeUtils.getBoxedTypeName(ownerType.toString());
                }
            } else {
                ownerQualifiedName = "";
            }
        }
        return ownerQualifiedName;
    }
}
