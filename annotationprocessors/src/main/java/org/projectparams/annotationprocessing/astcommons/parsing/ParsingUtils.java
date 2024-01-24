package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.utils.ElementUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ParsingUtils {
    public static List<String> getClassnamesInPackage(String packageName) {
        return ElementUtils.getPackageByName(packageName).getEnclosedElements().stream()
                .filter(el -> el.getKind() == ElementKind.CLASS)
                .map(el -> ((TypeElement) el).getQualifiedName().toString())
                .toList();
    }

    public static List<String> getStaticClassMembers(String className) {
        var classSymbol = ElementUtils.getClassByName(className);
        return classSymbol.getEnclosedElements().stream()
                .filter(el -> el.getModifiers().contains(Modifier.STATIC))
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
                return ParsingUtils.getClassnamesInPackage(packageName).stream();
            } else {
                return Stream.of(imp.getQualifiedIdentifier().toString());
            }
        }
        throw new UnsupportedOperationException("Unsupported import type: " + imp.getQualifiedIdentifier().getClass());
    }

    private static Stream<String> mapStaticImportToImportedNames(ImportTree imp) {
        if (imp.getQualifiedIdentifier() instanceof JCTree.JCFieldAccess fieldAccess) {
            if (fieldAccess.getIdentifier().contentEquals("*")) {
                return ParsingUtils.getStaticClassMembers(fieldAccess.getExpression().toString()).stream();
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
}
