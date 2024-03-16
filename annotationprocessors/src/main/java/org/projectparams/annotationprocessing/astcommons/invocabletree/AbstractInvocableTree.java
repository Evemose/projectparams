package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import java.util.List;

public abstract class AbstractInvocableTree<T extends ExpressionTree> implements InvocableTree {
    protected final T wrapped;
    protected final TreePath pathToWrapped;

    public AbstractInvocableTree(T wrapped, TreePath pathToWrapped) {
        this.wrapped = wrapped;
        this.pathToWrapped = pathToWrapped;
    }

    @Override
    public JCTree getWrapped() {
        return (JCTree) wrapped;
    }

    @Override
    public Kind getKind() {
        return wrapped.getKind();
    }

    @Override
    public <R, D> R accept(TreeVisitor<R, D> visitor, D data) {
        return wrapped.accept(visitor, data);
    }

    @Override
    public String toString() {
        return getOwnerTypeQualifiedName() + "." + getSelfName() + "(" + getArguments().stream().map(
                arg -> arg + ": " + TypeUtils.getActualType(arg)).reduce((a, b) -> a + ", " + b).orElse("")
                + ")" + ": " + getReturnType();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof AbstractInvocableTree)) return false;
        if (this.wrapped == ((AbstractInvocableTree<?>) obj).wrapped) return true;
        return wrapped.equals(obj);
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }

    @Override
    public Type getReturnType() {
        return ((JCTree.JCExpression) wrapped).type;
    }

    @Override
    public void setReturnType(Type type) {
        ((JCTree.JCExpression) wrapped).type = type;
    }

    @Override
    public void setReturnType(String type) {
        setReturnType(TypeUtils.getTypeByName(type));
    }

    @Override
    public List<Type> getArgumentTypes() {
        return getArguments().stream().map(TypeUtils::getActualType).toList();
    }

    @Override
    public TreePath getPath() {
        return pathToWrapped;
    }

}
