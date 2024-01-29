package org.projectparams.annotationprocessing.astcommons.context;

import com.sun.source.tree.CompilationUnitTree;

import java.util.*;

public record CUContext(
        Set<String> importedClassNames,
        Set<String> importedStaticMethods,
        Set<String> importedFields
) {
    public static CUContext from(CompilationUnitTree compilationUnitTree) {
        return new CUContext(
                ContextUtils.getImportedClassNames(compilationUnitTree),
                ContextUtils.getImportedStaticMethods(compilationUnitTree),
                ContextUtils.getImportedFields(compilationUnitTree)
        );
    }

    public Optional<String> getMatchingImportedStaticMethod(String methodName) {
        return getMatch(importedStaticMethods, methodName);
    }

    public Optional<String> getMatchingImportedOrStaticClass(String name) {
        var fieldsAndClasses = new ArrayList<>(importedFields);
        fieldsAndClasses.addAll(importedClassNames);
        return getMatch(fieldsAndClasses, name);
    }

    private Optional<String> getMatch(Collection<String> imports, String nameToMatch) {
        return imports.stream()
                .filter(importedName -> importedName.endsWith(nameToMatch))
                .findAny();
    }


}
