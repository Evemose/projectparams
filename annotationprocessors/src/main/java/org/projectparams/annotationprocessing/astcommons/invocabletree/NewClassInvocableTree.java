package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.utils.ElementUtils;

import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.List;

public class NewClassInvocableTree extends AbstractInvocableTree<NewClassTree> {
    public NewClassInvocableTree(NewClassTree wrapped, TreePath pathToWrapped) {
        super(wrapped, pathToWrapped);
        initializeDummyTypeIfNeeded();
    }

    @Override
    public String getSelfName() {
        return "<init>";
    }

    @Override
    public String getOwnerTypeQualifiedName() {
        var asJC = (JCTree.JCNewClass) wrapped;
        var typeIdentifier = asJC.getIdentifier();
        if (typeIdentifier.type == null) {
            TypeUtils.attributeExpression(asJC, pathToWrapped);
        }
        return typeIdentifier.type.toString();
    }

    @Override
    public List<? extends ExpressionTree> getArguments() {
        return wrapped.getArguments();
    }

    // cant do it in constructor cause constructor arg types are for some reason not initialized
    private void initializeDummyTypeIfNeeded() {
        var asJC = (JCTree.JCNewClass) wrapped;
        // initialize dummy type for method invocation
        if (asJC.constructorType == null) {
            var newClassName = getOwnerTypeQualifiedName();
            if (newClassName.startsWith("<any>")) {
                return;
            }
            try {
                var newClassType = TypeUtils.getTypeByName(newClassName);
                TypeUtils.updateIdentifierType(wrapped, newClassType);
            } catch (Exception ignored) {
            }
        }
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

    /**
     * Ignores the return type update
     * but throws if it's not the owner type
     */
    @Override
    public void setReturnType(String returnType) {
        if (!returnType.equals(getOwnerTypeQualifiedName())) {
            throw new IllegalArgumentException("Cannot set return type of constructor to anything other than " +
                    "the owner type. Got: " + returnType + " for " + wrapped);
        }
    }

    /**
     * Return type of constructor is the owner type
     * even though internally it's void
     * @return owner type
     */
    @Override
    public Type getReturnType() {
        return TypeUtils.getTypeByName(getOwnerTypeQualifiedName());
    }
}
