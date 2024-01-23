package org.projectparams.annotationprocessing.astcommons;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;

import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;

public class TypeUtils {
    private static Trees trees;
    private static JavacTypes types;
    private static Names names;
    private static Elements elements;
    private static TreeMaker treeMaker;
    private static Symtab symtab;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Trees trees, JavacTypes types, Names names, Elements elements, TreeMaker treeMaker, Symtab symtab) {
        TypeUtils.trees = trees;
        TypeUtils.types = types;
        TypeUtils.names = names;
        TypeUtils.elements = elements;
        TypeUtils.treeMaker = treeMaker;
        TypeUtils.symtab = symtab;
    }

    public static TypeMirror getReturnType(MethodInvocationTree methodInvocation, TreePath closestEnclosingElementPath) {
        return ((ExecutableType) trees.getTypeMirror(new TreePath(closestEnclosingElementPath, methodInvocation))).getReturnType();
    }

    public static Type getTypeByName(String name) {
        return (Type) switch (name){
            case "int" -> symtab.intType;
            case "long" -> symtab.longType;
            case "float" -> symtab.floatType;
            case "double" -> symtab.doubleType;
            case "boolean" -> symtab.booleanType;
            case "void" -> symtab.voidType;
            case "superSecretDefaultValuePlaceholder" -> symtab.botType;
            default -> types.getDeclaredType(elements.getTypeElement(name));
        };
    }

    public boolean isAssignable(String source, String target) {
        return types.isAssignable(getTypeByName(source), getTypeByName(target));
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
