package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.tree.CompilationUnitTree;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public record CUContext(
        // not sure if we need importedClassNames
        List<String> importedClassNames,
        List<String> importedNestedClasses,
        List<String> importedStaticMethods
) {
    public static CUContext from(CompilationUnitTree compilationUnitTree) {
        return new CUContext(
                ParsingUtils.getImportedClassNames(compilationUnitTree),
                ParsingUtils.getImportedNestedClasses(compilationUnitTree),
                ParsingUtils.getImportedStaticMethods(compilationUnitTree)
        );
    }

    @SuppressWarnings("unused")
    public Optional<String> getMatchingImportedClass(String className) {
        return getMatch(importedClassNames, className);
    }

    @SuppressWarnings("unused")
    public Optional<String> getMatchingImportedNestedClass(String className) {
        return getMatch(importedNestedClasses, className);
    }

    public Optional<String> getMatchingImportedStaticMethod(String methodName) {
        return getMatch(importedStaticMethods, methodName);
    }

    private Optional<String> getMatch(Collection<String> imports, String nameToMatch) {
        return imports.stream()
                .filter(importedName -> importedName.endsWith(nameToMatch))
                .findAny();
    }


}
