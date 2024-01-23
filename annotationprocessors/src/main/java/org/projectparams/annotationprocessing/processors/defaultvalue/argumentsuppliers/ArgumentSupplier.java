package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.MethodInfo;

public interface ArgumentSupplier {
    List<JCTree.JCExpression> getModifiedArguments(MethodInvocationTree invocation, MethodInfo methodInfo) throws UnsupportedSignatureException;
}
