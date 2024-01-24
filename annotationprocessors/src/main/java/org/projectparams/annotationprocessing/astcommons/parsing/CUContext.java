package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.source.tree.CompilationUnitTree;

import java.util.List;

public record CUContext(
        List<String> importedClassNames
) {
    public static CUContext from(CompilationUnitTree compilationUnitTree) {
        return new CUContext(
                ParsingUtils.getImportedClassNames(compilationUnitTree)
        );
    }


}
