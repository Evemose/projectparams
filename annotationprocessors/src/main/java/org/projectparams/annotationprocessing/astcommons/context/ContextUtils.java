package org.projectparams.annotationprocessing.astcommons.context;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.utils.ElementUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.*;
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

    public static List<String> getStaticClassMembersNames(String className, ElementKind ...excludedKinds) {
        var excludedKindsSet = Set.of(excludedKinds);
        return getStaticClassMembers(className).stream()
                .<String>mapMulti((el, consumer) -> {
                    if (el.getKind() == ElementKind.CLASS) {
                        consumer.accept(((TypeElement) el).getQualifiedName().toString());
                    } else if (!excludedKindsSet.contains(el.getKind())) {
                        consumer.accept(className + '.' + el.getSimpleName().toString());
                    }
                })
                .toList();
    }

    public static Set<String> getImportedClassNames(CompilationUnitTree compilationUnitTree) {
        var explicitImports = new HashSet<>(compilationUnitTree.getImports().stream()
                .flatMap(imp ->
                        imp.isStatic() ? mapStaticImportToImportedNames(imp, Symbol.MethodSymbol.class)
                                : mapImportToImportedNames(imp))
                .toList());
        // implicit imports are top-level classes in the same package as the compilation unit
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

    private static final Map<Class<?>, Set<ElementKind>> symbolTypeToElementKind = Map.of(
            Symbol.ClassSymbol.class, Set.of(ElementKind.CLASS, ElementKind.ENUM, ElementKind.INTERFACE, ElementKind.ANNOTATION_TYPE),
            Symbol.MethodSymbol.class, Set.of(ElementKind.METHOD),
            Symbol.VarSymbol.class, Set.of(ElementKind.FIELD)
    );

    private static Stream<String> mapStaticImportToImportedNames(ImportTree imp, Class<?> ...excludedSymbolTypes) {
        var excludedSymbolTypesSet = Set.of(excludedSymbolTypes);
        if (imp.getQualifiedIdentifier() instanceof JCTree.JCFieldAccess fieldAccess) {
            if (fieldAccess.getIdentifier().contentEquals("*")) {
                return ContextUtils.getStaticClassMembersNames(fieldAccess.getExpression().toString(),
                        excludedSymbolTypesSet.stream().flatMap(clazz -> symbolTypeToElementKind.get(clazz).stream())
                                .toArray(ElementKind[]::new)).stream();
            } else {
                if (fieldAccess.sym == null) {
                    fieldAccess.sym = fieldAccess.selected.type.tsym;
                }
                if (excludedSymbolTypesSet.contains(fieldAccess.sym.getClass())) {
                    return Stream.empty();
                }
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


    public static Set<String> getImportedStaticMethods(CompilationUnitTree compilationUnitTree) {
        return getImports(compilationUnitTree, ElementKind.METHOD);
    }

    public static Set<ClassContext.Method> getMethodsInClass(TreePath classPath) {
        return getMembersOfClass(classPath, ElementKind.METHOD).stream()
                .map(el -> (ClassContext.Method) el)
                .collect(Collectors.toSet());
    }

    public static Set<ClassContext.Field> getFieldsInClass(TreePath classPath) {
        return getMembersOfClass(classPath, ElementKind.FIELD).stream()
                .map(el -> (ClassContext.Field) el)
                .collect(Collectors.toSet());
    }

    public static Set<ClassContext.ClassMember> getMembersOfClass(TreePath classPath, ElementKind kind) {
        var decl = (JCTree.JCClassDecl) classPath.getLeaf();
        var classSymbol = decl.sym;
        var elements =  classSymbol.getEnclosedElements().stream()
                .filter(el -> el.getKind() == kind)
                .map(el -> ClassContext.of(
                        el.getSimpleName().toString(),
                        classSymbol.getQualifiedName().toString(),
                        el.getModifiers().contains(Modifier.STATIC),
                        kind))
                .collect(Collectors.toSet());
        if (decl.extending != null && !decl.mods.getFlags().contains(Modifier.STATIC)) {
            elements.addAll(getMembersOfClass(PathUtils.getEnclosingClassPath(classPath), kind));
        }
        return elements;
    }

    public static Set<String> getImportedFields(CompilationUnitTree compilationUnitTree) {
        return getImports(compilationUnitTree, ElementKind.FIELD);
    }

    public static Set<String> getImports(CompilationUnitTree compilationUnitTree, ElementKind elementKind) {
        return compilationUnitTree.getImports().stream()
                .filter(ImportTree::isStatic)
                .flatMap(imp -> {
                    var importString = imp.getQualifiedIdentifier().toString();
                    if (importString.endsWith("*")) {
                        return getStaticClassMembers(importString.substring(0, importString.length() - 2))
                                .stream()
                                .filter(el -> el.getKind() == elementKind)
                                .map(el -> ((Symbol.VarSymbol) el).owner.getQualifiedName() + "."
                                        + el.getSimpleName());
                    } else {
                        var ident = (JCTree.JCFieldAccess) imp.getQualifiedIdentifier();
                        if (ident.sym == null) {
                            ident.sym = ident.selected.type.tsym;
                        }
                        var member = getStaticClassMembers(ident.selected.toString()).stream()
                                .filter(el -> el.getKind() == elementKind && el.getSimpleName().toString().equals(ident.getIdentifier().toString()))
                                .findAny();
                        return member.stream().map(element -> importString);
                    }
                })
                .collect(Collectors.toSet());
    }
}
