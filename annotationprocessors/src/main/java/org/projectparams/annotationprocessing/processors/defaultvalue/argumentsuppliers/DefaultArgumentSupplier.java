package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.ExpressionMaker;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.parsing.ParsedExpression;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

import java.util.ArrayList;
import java.util.Collections;

public class DefaultArgumentSupplier implements ArgumentSupplier {

    @Override
    public List<JCTree.JCExpression> getModifiedArguments(InvocableTree invocation, InvocableInfo invocableInfo)
            throws UnsupportedSignatureException {
        var args = new ArrayList<>(invocation.getArguments().stream().map(arg -> (JCTree.JCExpression) arg).toList());
        for (var i = invocation.getArguments().size(); i < invocableInfo.parameterTypeQualifiedNames().size(); i++) {
            var defaultValue = invocableInfo.paramIndexToDefaultValue().get(i);
            if (defaultValue == null) {
                throw new UnsupportedSignatureException(invocableInfo.parameterTypeQualifiedNames().get(i), i, invocableInfo);
            }
            args.add(ExpressionMaker.makeExpr(ParsedExpression.from(defaultValue)));
        }
        return List.from(args);
    }

}
