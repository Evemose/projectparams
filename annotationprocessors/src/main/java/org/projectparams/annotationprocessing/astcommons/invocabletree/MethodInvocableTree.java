package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;

import java.util.Arrays;
import java.util.List;

public class MethodInvocableTree extends AbstractInvocableTree<MethodInvocationTree> {

    public MethodInvocableTree(MethodInvocationTree methodInvocationTree, TreePath pathToMethod) {
        super(methodInvocationTree, pathToMethod);

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
        return TypeUtils.getOwnerTypeName(wrapped, pathToWrapped);
    }

    @Override
    public void setReturnType(String returnType) {
        super.setReturnType(returnType);
        var asJC = (JCTree.JCMethodInvocation) wrapped;
        asJC.meth.type = new Type.MethodType(
                asJC.meth.type.getParameterTypes(),
                TypeUtils.getTypeByName(returnType),
                asJC.meth.type.getThrownTypes(),
                asJC.meth.type.tsym);
        asJC.type = TypeUtils.getTypeByName(returnType);
    }

    @Override
    public List<? extends ExpressionTree> getArguments() {
        return wrapped.getArguments();
    }

    @Override
    public void setArguments(ExpressionTree... arguments) {
        var asJC = (JCTree.JCMethodInvocation) wrapped;
        asJC.args = com.sun.tools.javac.util.List.from(Arrays.stream(arguments).map(arg -> (JCTree.JCExpression) arg).toList());
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
}
