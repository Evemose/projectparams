package org.projectparams.annotationprocessing.astcommons.parsing;

import com.sun.tools.javac.tree.JCTree;

public interface Expression {
    JCTree.JCExpression toJcExpression();
    default Expression getRootOwner() {
        return this;
    }
}
