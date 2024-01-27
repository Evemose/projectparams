package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

public interface ArgumentSupplier {
    List<JCTree.JCExpression> getModifiedArguments(InvocableTree invocation,
                                                   InvocableInfo invocableInfo,
                                                   TreePath path) throws UnsupportedSignatureException;
}
