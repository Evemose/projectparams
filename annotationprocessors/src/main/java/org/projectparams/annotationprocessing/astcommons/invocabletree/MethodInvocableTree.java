package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Name;
import org.projectparams.annotationprocessing.astcommons.PathUtils;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;
import org.projectparams.annotationprocessing.astcommons.parsing.utils.ExpressionMaker;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    public InvocableTree withActualTypes(Map<Name, Type> conversionMap) {
        TypeUtils.attributeExpression((JCTree) wrapped, pathToWrapped);
        var asJC = (JCTree.JCMethodInvocation) wrapped;
        var newAsJC = ExpressionMaker.makeMethodInvocation(
                asJC.meth,
                asJC.typeargs,
                asJC.args.map(arg -> {
                    var newArg = ExpressionMaker.makeIdent(arg.toString());
                    if (arg.type != null) {
                        newArg.type = conversionMap.getOrDefault(arg.type.tsym.name, arg.type);
                    }
                    return newArg;
                }).toArray(JCTree.JCExpression[]::new)
        );
        newAsJC.type = conversionMap.getOrDefault(asJC.type.tsym.name, asJC.type);
        return new MethodInvocableTree(newAsJC, pathToWrapped);
    }

    @Override
    public List<JCTree> getTypeArguments() {
        return wrapped.getTypeArguments().stream().map(JCTree.class::cast).toList();
    }

    @Override
    public ExpressionTree getOwner() {
        var meth = ((JCTree.JCMethodInvocation) wrapped).meth;
        if (meth instanceof JCTree.JCFieldAccess fieldAccess) {
            return fieldAccess.selected;
        } else {
            return ClassContext.of(PathUtils.getEnclosingClassPath(pathToWrapped))
                    .getMatchingMethod(meth.toString())
                    .map(m -> ExpressionMaker.makeIdent(m.className()))
                    .orElseThrow();
        }
    }

    @Override
    public List<? extends ExpressionTree> getArguments() {
        return wrapped.getArguments();
    }

    @Override
    public void setArguments(ExpressionTree... arguments) {
        var asJC = (JCTree.JCMethodInvocation) wrapped;
        asJC.args = com.sun.tools.javac.util.List.from(Arrays.stream(arguments).map(JCTree.JCExpression.class::cast).toList());
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
