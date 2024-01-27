package org.projectparams.annotationprocessing.astcommons.context;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.utils.ElementUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContextUtils {
    public static List<String> getClassnamesInPackage(String packageName) {
        return ElementUtils.getPackageByName(packageName).getEnclosedElements().stream()
                .filter(el -> el.getKind() == ElementKind.CLASS)
                .map(el -> ((TypeElement) el).getQualifiedName().toString())
                .toList();
    }

    public static List<? extends Element> getStaticClassMembers(String className) {
        var classSymbol = ElementUtils.getClassByName(className);
        return classSymbol.getEnclosedElements().stream()
                .filter(el -> el.getModifiers().contains(Modifier.STATIC))
                .toList();
    }

    public static List<String> getStaticClassMembersNames(String className) {
        return getStaticClassMembers(className).stream()
                .map(el -> {
                    if (el.getKind() == ElementKind.CLASS) {
                        return ((TypeElement) el).getQualifiedName().toString();
                    } else {
                        return className + '.' + el.getSimpleName().toString();
                    }
                })
                .toList();
    }

    public static List<String> getImportedClassNames(CompilationUnitTree compilationUnitTree) {
        var explicitImports = new ArrayList<>(compilationUnitTree.getImports().stream()
                .flatMap(imp ->
                        imp.isStatic() ? mapStaticImportToImportedNames(imp) : mapImportToImportedNames(imp))
                .toList());
        // implicit imports are classes in the same package as the compilation unit
        var implicitImports = ElementUtils.getPackageByName(compilationUnitTree.getPackageName().toString())
                .getEnclosedElements().stream()
                .filter(el -> el.getKind() == ElementKind.CLASS)
                .map(el -> ((TypeElement) el).getQualifiedName().toString())
                .toList();
        explicitImports.addAll(implicitImports);
        return explicitImports;
    }

    private static Stream<String> mapImportToImportedNames(ImportTree imp) {
        if (((JCTree.JCImport) imp).getQualifiedIdentifier() instanceof JCTree.JCFieldAccess fieldAccess) {
            // add all classes from the package (wildcard import)
            if (fieldAccess.getIdentifier().contentEquals("*")) {
                var packageName = fieldAccess.getExpression().toString();
                return ContextUtils.getClassnamesInPackage(packageName).stream();
            } else {
                return Stream.of(imp.getQualifiedIdentifier().toString());
            }
        }
        throw new UnsupportedOperationException("Unsupported import type: " + imp.getQualifiedIdentifier().getClass());
    }

    private static Stream<String> mapStaticImportToImportedNames(ImportTree imp) {
        if (imp.getQualifiedIdentifier() instanceof JCTree.JCFieldAccess fieldAccess) {
            if (fieldAccess.getIdentifier().contentEquals("*")) {
                return ContextUtils.getStaticClassMembersNames(fieldAccess.getExpression().toString()).stream();
            } else {
                return Stream.of(switch (fieldAccess.sym) {
                    case Symbol.ClassSymbol classSymbol -> classSymbol.getQualifiedName().toString();
                    case Symbol.MethodSymbol methodSymbol -> methodSymbol.owner.getQualifiedName() + "."
                            + methodSymbol.getSimpleName();
                    case Symbol.VarSymbol varSymbol -> varSymbol.owner.getQualifiedName() + "."
                            + varSymbol.getSimpleName();
                    default ->
                            throw new UnsupportedOperationException("Unsupported symbol type: " + fieldAccess.sym.getClass());
                });
            }
        }
        throw new UnsupportedOperationException("Unsupported static import type: " + imp.getQualifiedIdentifier().getClass());
    }

    public static List<String> getImportedNestedClasses(CompilationUnitTree compilationUnitTree) {
        return compilationUnitTree.getImports().stream()
                .filter(ImportTree::isStatic)
                .flatMap(imp -> {
                    var importString = imp.getQualifiedIdentifier().toString();
                    if (importString.endsWith("*")) {
                        return getStaticClassMembers(importString.substring(0, importString.length() - 2))
                                .stream()
                                .filter(el -> el.getKind() == ElementKind.CLASS)
                                .map(el -> ((TypeElement) el).getQualifiedName().toString());
                    } else {
                        return Stream.of(importString);
                    }
                })
                .toList();
    }

    public static List<String> getImportedStaticMethods(CompilationUnitTree compilationUnitTree) {
        return compilationUnitTree.getImports().stream()
                .filter(ImportTree::isStatic)
                .flatMap(imp -> {
                    var importString = imp.getQualifiedIdentifier().toString();
                    if (importString.endsWith("*")) {
                        return getStaticClassMembers(importString.substring(0, importString.length() - 2))
                                .stream()
                                .filter(el -> el.getKind() == ElementKind.METHOD)
                                .map(el -> ((Symbol.MethodSymbol) el).owner.getQualifiedName() + "."
                                        + el.getSimpleName());
                    } else {
                        return Stream.of(importString);
                    }
                })
                .toList();
    }

    public static Set<ClassContext.Method> getMethodsInClass(TreePath classPath) {
        var classSymbol = (Symbol.ClassSymbol) ((JCTree.JCClassDecl)classPath.getLeaf()).sym;
        return classSymbol.getEnclosedElements().stream()
                .filter(el -> el.getKind() == ElementKind.METHOD)
                .map(el -> new ClassContext.Method(
                        el.getSimpleName().toString(),
                        classSymbol.getQualifiedName().toString(),
                        el.getModifiers().contains(Modifier.STATIC)))
                .collect(Collectors.toUnmodifiableSet());
    }
}
