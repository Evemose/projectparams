package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacTypes;

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
}
