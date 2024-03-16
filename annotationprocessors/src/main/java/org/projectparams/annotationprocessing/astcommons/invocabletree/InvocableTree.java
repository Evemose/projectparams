package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Name;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public interface InvocableTree extends ExpressionTree {
    String getSelfName();

    String getOwnerTypeQualifiedName();

    List<? extends ExpressionTree> getArguments();

    void setArguments(ExpressionTree... arguments);

    void setThrownTypes(Type... thrownTypes);

    void setThrownTypes(String... thrownTypeNames);

    JCTree getWrapped();

    Type getReturnType();

    void setReturnType(Type type);

    void setReturnType(String type);

    List<Type> getArgumentTypes();

    InvocableTree withActualTypes(Map<Name, Type> conversionMap);

    TreePath getPath();
}
