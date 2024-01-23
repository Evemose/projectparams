package org.projectparams.annotationprocessing.astcommons.invocabletree;

import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;


import java.util.List;

public interface InvocableTree extends ExpressionTree {
    String getSelfName();
    String getOwnerTypeQualifiedName();
    List<? extends ExpressionTree> getArguments();
    void setArguments(ExpressionTree ...arguments);
    void updateArgumentType(ExpressionTree argTree);
    void setTargetType(Type type);
    void setReturnType(Type type);
    void setTargetType(String typeQualifiedName);
    void setType(MethodInfo methodInfo);
    void setThrownTypes(Type ...thrownTypes);
    void setThrownTypes(String ...thrownTypes);
    ExpressionTree getWrapped();
    Type getWrappedType();
    Type getReturnType();
}
