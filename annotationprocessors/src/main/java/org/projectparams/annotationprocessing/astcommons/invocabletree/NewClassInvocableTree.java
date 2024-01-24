package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.CUContext;

import java.util.Arrays;
import java.util.List;

public class NewClassInvocableTree extends AbstractInvocableTree<NewClassTree> {
    public NewClassInvocableTree(NewClassTree wrapped, TreePath pathToWrapped) {
        super(wrapped, pathToWrapped);
    }

    @Override
    public String getSelfName() {
        return "<init>";
    }

    @Override
    public String getOwnerTypeQualifiedName() {
        var asJC = (JCTree.JCNewClass) wrapped;
        var typeIdentifier = asJC.getIdentifier();
        return switch (typeIdentifier.getKind()) {
            case IDENTIFIER -> {
                var cuContext = CUContext.from(pathToWrapped.getCompilationUnit());
                yield cuContext.importedClassNames().stream()
                        .filter(imp -> imp.endsWith("." + typeIdentifier))
                        .findAny().orElseThrow(() -> new RuntimeException("No matching import found for " + typeIdentifier));
            }
            case MEMBER_SELECT -> ((MemberSelectTree) typeIdentifier).toString();
            case PARAMETERIZED_TYPE -> ((ParameterizedTypeTree) typeIdentifier).getType().toString();
            default -> throw new UnsupportedOperationException("Type extraction not supported for trees of type " +
                    typeIdentifier.getKind());
        };
    }

    @Override
    public List<? extends ExpressionTree> getArguments() {
        return wrapped.getArguments();
    }

    @Override
    public void setArguments(ExpressionTree... arguments) {
        var asJC = (JCTree.JCNewClass) wrapped;
        asJC.args = com.sun.tools.javac.util.List.from(
                Arrays.stream(arguments).map(t -> (JCTree.JCExpression) t).toArray(JCTree.JCExpression[]::new));
    }

    @Override
    public void setThrownTypes(Type... thrownTypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setThrownTypes(String... thrownTypeNames) {
        setThrownTypes(Arrays.stream(thrownTypeNames).map(TypeUtils::getTypeByName).toArray(Type[]::new));
    }
}
