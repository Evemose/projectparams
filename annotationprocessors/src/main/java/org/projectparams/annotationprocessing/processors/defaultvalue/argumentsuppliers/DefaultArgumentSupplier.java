package org.projectparams.annotationprocessing.processors.defaultvalue.argumentsuppliers;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.projectparams.annotationprocessing.astcommons.TypeUtils;
import org.projectparams.annotationprocessing.astcommons.invocabletree.InvocableTree;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.ExpressionFactory;
import org.projectparams.annotationprocessing.astcommons.parsing.expressions.LiteralExpression;
import org.projectparams.annotationprocessing.exceptions.UnsupportedSignatureException;
import org.projectparams.annotationprocessing.processors.defaultvalue.InvocableInfo;

import java.util.ArrayList;

public class DefaultArgumentSupplier implements ArgumentSupplier {

    @Override
    public List<JCTree.JCExpression> getModifiedArguments(InvocableTree invocation,
                                                          InvocableInfo invocableInfo,
                                                          TreePath path)
            throws UnsupportedSignatureException {
        var args = new ArrayList<>(invocation.getArguments().stream().map(arg -> (JCTree.JCExpression) arg).toList());
        for (var i = invocation.getArguments().size(); i < invocableInfo.parameters().size(); i++) {
            var defaultValue = invocableInfo.parameters().get(i).defaultValue();
            if (defaultValue == null) {
                throw new UnsupportedSignatureException(invocableInfo.parameters().get(i).name(), i, invocableInfo);
            }
            var expression = ExpressionFactory.createExpression(defaultValue.expression(),
                    TypeUtils.getUnboxedTypeTag(defaultValue.type()), path);
            if (expression instanceof LiteralExpression) {
                args.add(expression.toJcExpression());
            } else {
                args.add(LiteralExpression.NULL.toJcExpression());
            }
        }
        return List.from(args);
    }

}
