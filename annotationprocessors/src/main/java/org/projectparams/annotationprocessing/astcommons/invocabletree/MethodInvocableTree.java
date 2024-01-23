package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;

import java.util.Arrays;
import java.util.List;

public class MethodInvocableTree implements InvocableTree {
    private final MethodInvocationTree wrapped;
    private final TreePath pathToMethod;

    public MethodInvocableTree(MethodInvocationTree methodInvocationTree, TreePath pathToMethod) {
        this.wrapped = methodInvocationTree;
        this.pathToMethod = pathToMethod;

        var asJC = (JCTree.JCMethodInvocation) wrapped;
        // initialize dummy type for method invocation
        if (asJC.meth.type == null) {
            asJC.meth.type = new Type.MethodType(
                    com.sun.tools.javac.util.List.nil(),
                    Type.noType,
                    com.sun.tools.javac.util.List.nil(),
                    null);
        }
    }

    @Override
    public String getSelfName() {
        // methodSelect represents the method signature before the arguments list (invocation target name + method name)
        // so the last part of the method select is the method name
        var split = wrapped.getMethodSelect().toString().split("\\.");
        return split[split.length - 1];
    }

    @Override
    public String getOwnerTypeQualifiedName() {
        return TypeUtils.getOwnerTypeName(wrapped, pathToMethod);
    }

    @Override
    public List<? extends ExpressionTree> getArguments() {
        return wrapped.getArguments();
    }

    @Override
    public void setArguments(ExpressionTree ...arguments) {
        var asJC = (JCTree.JCMethodInvocation) wrapped;
        asJC.args = com.sun.tools.javac.util.List.from(Arrays.stream(arguments).map(arg -> (JCTree.JCExpression) arg).toList());
    }


    @Override
    public void setReturnType(Type type) {
        var asJC = (JCTree.JCMethodInvocation) wrapped;
        asJC.type = type;
    }

    @Override
    public void setReturnType(String type) {
        setReturnType(TypeUtils.getTypeByName(type));
    }

    @Override
    public void setThrownTypes(Type... thrownTypes) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setThrownTypes(String... thrownTypeNames) {
        Arrays.stream(thrownTypeNames).map(TypeUtils::getTypeByName)
                .forEach(this::setThrownTypes);
    }

    @Override
    public ExpressionTree getWrapped() {
        return wrapped;
    }

    @Override
    public Type getWrappedType() {
        return ((JCTree.JCMethodInvocation) wrapped).meth.type;
    }

    @Override
    public Type getReturnType() {
        return ((JCTree.JCMethodInvocation) wrapped).type;
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
        return wrapped.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodInvocableTree that)) return false;
        return wrapped.equals(that.wrapped);
    }

    @Override
    public int hashCode() {
        return wrapped.hashCode();
    }
}
