package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
                Arrays.stream(arguments).map(JCTree.JCExpression.class::cast).toArray(JCTree.JCExpression[]::new));
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

    @Override
    public InvocableTree withActualTypes(Map<Name, Type> conversionMap) {
        var asJC = (JCTree.JCNewClass) wrapped;
        var newAsJc = ExpressionMaker.makeNewClass(
                asJC.encl,
                asJC.clazz.toString(),
                asJC.typeargs,
                asJC.args.toArray(new JCTree.JCExpression[0])
        );
        return new NewClassInvocableTree(newAsJc, pathToWrapped);
    }

    @Override
    public List<JCTree> getTypeArguments() {
        return wrapped.getTypeArguments().stream().map(JCTree.class::cast).toList();
    }

    @Override
    public ExpressionTree getOwner() {
        return ((JCTree.JCNewClass) wrapped).clazz;
    }
}
