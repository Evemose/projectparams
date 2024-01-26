package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import javax.annotation.processing.Messager;
import java.util.Arrays;
import java.util.List;

public class NewClassInvocableTree extends AbstractInvocableTree<NewClassTree> {

    public static Messager messager;

    public NewClassInvocableTree(NewClassTree wrapped, TreePath pathToWrapped) {
        super(wrapped, pathToWrapped);
    }

    @Override
    public String getSelfName() {
        return "<init>";
    }

    @Override
    public String getOwnerTypeQualifiedName() {
        return TypeUtils.getOwnerTypeName(wrapped);
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
        asJC.constructorType = new Type.MethodType(
                com.sun.tools.javac.util.List.from(
                        Arrays.stream(arguments).map(t -> ((JCTree.JCExpression) t).type)
                                .toList()),
                asJC.constructorType.getReturnType(),
                asJC.constructorType.getThrownTypes(),
                asJC.constructorType.tsym);
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
     * Return type of constructor is the owner type
     * even though internally it's void
     *
     * @return owner type
     */
    @Override
    public Type getReturnType() {
        var ownerName = getOwnerTypeQualifiedName();
        if (ownerName.startsWith("<any>")) {
            return null;
        }
        try {
            return TypeUtils.getTypeByName(ownerName);
        } catch (Exception e) {
            return null;
        }
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
}
