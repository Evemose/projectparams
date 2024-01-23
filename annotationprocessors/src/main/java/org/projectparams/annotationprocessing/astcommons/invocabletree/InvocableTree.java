package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;


import java.util.List;

@SuppressWarnings("unused")
public interface InvocableTree extends ExpressionTree {
    String getSelfName();
    String getOwnerTypeQualifiedName();
    List<? extends ExpressionTree> getArguments();
    void setArguments(ExpressionTree ...arguments);
    void setReturnType(Type type);
    void setReturnType(String type);
    void setThrownTypes(Type ...thrownTypes);
    void setThrownTypes(String ...thrownTypeNames);
    ExpressionTree getWrapped();
    Type getWrappedType();
    Type getReturnType();
}
