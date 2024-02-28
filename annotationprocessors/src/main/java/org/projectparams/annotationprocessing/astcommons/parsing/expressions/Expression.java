package org.projectparams.annotationprocessing.astcommons.parsing.expressions;

import com.sun.tools.javac.tree.JCTree;
import org.projectparams.annotationprocessing.astcommons.context.ClassContext;

public interface Expression {
    JCTree.JCExpression toJcExpression();

    void convertIdentsToQualified(ClassContext classContext);
}
