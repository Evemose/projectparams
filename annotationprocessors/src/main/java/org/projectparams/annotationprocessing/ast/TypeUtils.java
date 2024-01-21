package org.projectparams.annotationprocessing.ast;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.util.Names;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class TypeUtils {
    private static Trees trees;
    private static JavacTypes types;
    private static Names names;
    private static Elements elements;

    // initialized in org.projectparams.annotationprocessing.MainProcessor
    public static void init(Trees trees, JavacTypes types, Names names, Elements elements) {
        TypeUtils.trees = trees;
        TypeUtils.types = types;
        TypeUtils.names = names;
        TypeUtils.elements = elements;
    }

    public static String getQualifiedTypeName(Element element) {
        var type = element.asType();
        return type.toString();
    }

    public static TypeMirror getReturnType(MethodInvocationTree methodInvocation, TreePath closestEnclosingElementPath) {
        return ((ExecutableType) trees.getTypeMirror(new TreePath(closestEnclosingElementPath, methodInvocation))).getReturnType();
    }

    public static Type getTypeByName(String name) {
        var typeElement = elements.getTypeElement(name);
        return (Type) types.getDeclaredType(typeElement);
    }
}
